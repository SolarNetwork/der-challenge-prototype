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

import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import net.solarnetwork.esi.util.Callback;

/**
 * Utilities for simplifying transactional-aware code.
 * 
 * @author matt
 * @version 1.0
 */
@ParametersAreNonnullByDefault
public final class TransactionUtils {

  /**
   * Perform a callback after a transaction commit.
   * 
   * <p>
   * If there is no active transaction, the callback will be invoked immediately.
   * </p>
   * 
   * @param callback
   *        the callback to invoke
   */
  public static void afterCommit(Callback callback) {
    afterPhase(TransactionPhase.AFTER_COMMIT, callback);
  }

  /**
   * Perform a callback after a specific transaction phase.
   * 
   * <p>
   * If there is no active transaction, the callback will be invoked immediately.
   * </p>
   * 
   * @param phase
   *        the phase to invoke the callback on
   * @param callback
   *        the callback to invoke
   */
  public static void afterPhase(TransactionPhase phase, Callback callback) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      registerSynchronization(new TransactionSynchronizationAdapter() {

        @Override
        public void beforeCommit(boolean readOnly) {
          if (phase == TransactionPhase.BEFORE_COMMIT) {
            callback.perform();
          }
        }

        @Override
        public void afterCompletion(int status) {
          if (phase == TransactionPhase.AFTER_COMPLETION
              || (phase == TransactionPhase.AFTER_ROLLBACK && status == STATUS_ROLLED_BACK)) {
            callback.perform();
          }
        }

        @Override
        public void afterCommit() {
          if (phase == TransactionPhase.AFTER_COMMIT) {
            callback.perform();
          }
        }

      });
    } else {
      callback.perform();
    }

  }

}
