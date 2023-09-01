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

package net.solarnetwork.esi.dao.support;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * ApplicationEventPublisher implementation that can synchronize the publishing of events to the
 * current thread's active transaction.
 * 
 * <p>
 * This can be useful for event handlers that need to access data related to the active transaction,
 * for example by querying the data after the transaction completes. If no transaction is available
 * when {@link #publishEvent(Object)} is called, it will be immediately passed to the configured
 * delegate {@link ApplicationEventPublisher}. Otherwise the event will be registered to publish
 * when the configured transaction phase occurs.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class TransactionalApplicationEventPublisher implements ApplicationEventPublisher {

  private final ApplicationEventPublisher delegate;

  private TransactionPhase phase = TransactionPhase.AFTER_COMPLETION;
  private int order = 0;

  /**
   * Constructor.
   * 
   * @param delegate
   *        the publisher to delegate to
   */
  public TransactionalApplicationEventPublisher(ApplicationEventPublisher delegate) {
    super();
    assert delegate != null;
    this.delegate = delegate;
  }

  @Override
  public void publishEvent(Object event) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronization transactionSynchronization = new Adapter(delegate, event, phase,
          order);
      TransactionSynchronizationManager.registerSynchronization(transactionSynchronization);
    } else {
      delegate.publishEvent(event);
    }
  }

  private static class Adapter implements TransactionSynchronization, Ordered {

    private final ApplicationEventPublisher eventAdmin;
    private final Object event;
    private final TransactionPhase phase;
    private int order = 0;

    public Adapter(ApplicationEventPublisher eventAdmin, Object event, TransactionPhase phase,
        int order) {
      this.eventAdmin = eventAdmin;
      this.event = event;
      this.phase = phase;
      this.order = order;
    }

    @Override
    public void beforeCommit(boolean readOnly) {
      if (phase == TransactionPhase.BEFORE_COMMIT) {
        processEvent();
      }
    }

    @Override
    public void afterCompletion(int status) {
      if (phase == TransactionPhase.AFTER_COMPLETION) {
        processEvent();
      } else if (phase == TransactionPhase.AFTER_COMMIT && status == STATUS_COMMITTED) {
        processEvent();
      } else if (phase == TransactionPhase.AFTER_ROLLBACK && status == STATUS_ROLLED_BACK) {
        processEvent();
      }
    }

    @Override
    public int getOrder() {
      return order;
    }

    protected void processEvent() {
      eventAdmin.publishEvent(event);
    }
  }

  /**
   * Set the transaction phase to associate posting events with.
   * 
   * <p>
   * This defaults to {@link TransactionPhase#AFTER_COMPLETION} so events are posted always, after
   * commit or roll back.
   * </p>
   * 
   * @param phase
   *        the phase to set
   */
  public void setPhase(TransactionPhase phase) {
    assert phase != null;
    this.phase = phase;
  }

  /**
   * Set the order to attach to synchronized events.
   * 
   * <p>
   * When events are synchronized to a transaction phase, this order will be used to rank all
   * registered tasks for that phase. That includes events posted by this service as well as any
   * other tasks registered with the transaction.
   * </p>
   * 
   * @param order
   *        the order to set
   */
  public void setOrder(int order) {
    this.order = order;
  }

}
