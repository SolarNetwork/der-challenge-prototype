/* ========================================================================
 * Copyright 2019 SolarNetwork Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */

package net.solarnetwork.esi.solarnet.fac.impl;

import static java.util.Arrays.asList;

import java.time.Instant;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferEventEntity;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferExecutionState;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferNotification.PriceMapOfferAccepted;
import net.solarnetwork.esi.solarnet.fac.service.PriceMapOfferExecutionService;

/**
 * Listens for accepted price map offers and schedules their execution.
 * 
 * @author matt
 * @version 1.0
 */
public class PriceMapOfferExecutionManager {

  private static final Logger log = LoggerFactory.getLogger(PriceMapOfferExecutionManager.class);

  private final PriceMapOfferExecutionService offerExecutionService;
  private final NavigableSet<PriceMapExecutionScheduledEvent> pending;
  private final Lock taskLock;
  private final Condition taskCondition;
  private final Thread taskThread;

  @ParametersAreNonnullByDefault
  private static final class PriceMapExecutionScheduledEvent
      implements Comparable<PriceMapExecutionScheduledEvent> {

    private final Instant date;
    private final UUID offerId;
    private final PriceMapOfferExecutionState newState;

    private PriceMapExecutionScheduledEvent(UUID offerId, Instant date,
        PriceMapOfferExecutionState newState) {
      super();
      this.offerId = offerId;
      this.date = date;
      this.newState = newState;
    }

    @Override
    public int compareTo(PriceMapExecutionScheduledEvent o) {
      if (o == null) {
        return 1; // nulls first
      }
      int result = date.compareTo(o.date);
      if (result == 0) {
        result = newState.compareTo(o.newState);
        if (result == 0) {
          result = offerId.compareTo(o.offerId);
        }
      }
      return result;
    }

    @Override
    public int hashCode() {
      return Objects.hash(offerId, date, newState);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof PriceMapExecutionScheduledEvent)) {
        return false;
      }
      PriceMapExecutionScheduledEvent other = (PriceMapExecutionScheduledEvent) obj;
      return Objects.equals(offerId, other.offerId) && Objects.equals(date, other.date)
          && Objects.equals(newState, other.newState);
    }

    @Override
    public String toString() {
      return "OfferExecution{" + offerId + " @ " + date + " ~ " + newState + "}";
    }

  }

  /**
   * Constructor.
   * 
   * @param offerExecutionService
   *        the offer execution service
   */
  public PriceMapOfferExecutionManager(PriceMapOfferExecutionService offerExecutionService) {
    super();
    this.offerExecutionService = offerExecutionService;
    this.pending = new ConcurrentSkipListSet<>();
    ReentrantLock l = new ReentrantLock();
    this.taskLock = l;
    this.taskCondition = l.newCondition();
    this.taskThread = new Thread(new TaskRunner(), "PriceMapOfferExecutionManager");
    this.taskThread.setDaemon(true);
    this.taskThread.start();
  }

  private static final UUID MAX_UUID = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
  private static final long PENDING_FUTURE_PADDING_MS = 200L;

  private class TaskRunner implements Runnable {

    private volatile boolean keepGoing = true;

    @Override
    public void run() {
      log.info("Starting price map offer execution manager thread.");
      while (keepGoing) {
        taskLock.lock();
        try {
          PriceMapExecutionScheduledEvent next = null;
          try {
            next = pending.first();
          } catch (NoSuchElementException e) {
            // ignore this
          }
          long sleepTime = (next != null ? next.date.toEpochMilli() - System.currentTimeMillis()
              : Integer.MAX_VALUE);
          if (sleepTime < 0) {
            sleepTime = 0;
          }
          log.debug("Waiting {}ms to execute next offer task: {}", sleepTime, next);
          taskCondition.await(sleepTime, TimeUnit.MILLISECONDS);
          if (keepGoing) {
            Set<PriceMapExecutionScheduledEvent> offersToManage = new HashSet<>();
            synchronized (pending) {
              // get all pending executions less than now, plus a small amount of padding to grap
              // executions "just about" to be ready to execute
              NavigableSet<PriceMapExecutionScheduledEvent> ready = pending
                  .headSet(new PriceMapExecutionScheduledEvent(MAX_UUID,
                      Instant.now().plusMillis(PENDING_FUTURE_PADDING_MS),
                      PriceMapOfferExecutionState.ABORTED), true);
              for (PriceMapExecutionScheduledEvent task : ready) {
                offersToManage.add(task);
              }
              ready.clear();
            }
            if (!offersToManage.isEmpty()) {
              log.info("Found {} offer tasks to manage: {}", offersToManage.size(), offersToManage);
              for (PriceMapExecutionScheduledEvent offer : offersToManage) {
                // TODO: handle results, and re-schedule if they don't complete;
                // must be handled by a different thread though
                if (offer.newState == PriceMapOfferExecutionState.EXECUTING) {
                  offerExecutionService.executePriceMapOfferEvent(offer.offerId);
                } else {
                  offerExecutionService.endPriceMapOfferEvent(offer.offerId, offer.newState);
                }
              }
            }
          }
        } catch (InterruptedException e) {
          // hello
        } finally {
          taskLock.unlock();
        }
      }
    }
  }

  /**
   * Handle a price map offer accepted event.
   * 
   * @param event
   *        the event
   */
  @Async
  @EventListener
  @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
  public void handlePriceMapOfferAccpeted(PriceMapOfferAccepted event) {
    PriceMapOfferEventEntity entity = event.getOfferEvent();
    if (!(entity.isAccepted()
        && entity.getExecutionState() == PriceMapOfferExecutionState.WAITING)) {
      return;
    }
    log.info("Scheduling execution task for offer {} @ {}", entity.getId(), entity.getStartDate());
    pending.addAll(asList(
        new PriceMapExecutionScheduledEvent(entity.getId(), entity.getStartDate(),
            PriceMapOfferExecutionState.EXECUTING),
        new PriceMapExecutionScheduledEvent(entity.getId(),
            entity.getStartDate().plus(entity.offerPriceMap().duration()),
            PriceMapOfferExecutionState.COMPLETED)));
    taskLock.lock();
    try {
      taskCondition.signal();
    } finally {
      taskLock.unlock();
    }
  }

}
