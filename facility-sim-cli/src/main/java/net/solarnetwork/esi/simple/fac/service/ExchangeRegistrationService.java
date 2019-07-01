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

package net.solarnetwork.esi.simple.fac.service;

import java.util.Locale;

import net.solarnetwork.esi.domain.DerFacilityExchangeInfo;
import net.solarnetwork.esi.domain.DerFacilityExchangeRequest;
import net.solarnetwork.esi.domain.DerFacilityRegistration;
import net.solarnetwork.esi.domain.DerFacilityRegistrationForm;
import net.solarnetwork.esi.domain.FormData;
import net.solarnetwork.esi.simple.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.simple.fac.domain.ExchangeRegistrationEntity;
import net.solarnetwork.esi.simple.fac.domain.ExchangeRegistrationEvent;

/**
 * API for a facility exchange registration service.
 * 
 * @author matt
 * @version 1.0
 */
public interface ExchangeRegistrationService {

  /**
   * Get a listing of available exchanges from the exchange registry.
   * 
   * @param criteria
   *        the search criteria
   * @return the results
   */
  Iterable<DerFacilityExchangeInfo> listExchanges(DerFacilityExchangeRequest criteria);

  /**
   * Get a listing of all available exchange registration entities.
   * 
   * <p>
   * These represent non-completed registrations, which have been submitted to an exchange via
   * {@link #registerWithExchange(DerFacilityExchangeInfo, FormData)} but who have not called the
   * {@link #completeExchangeRegistration(DerFacilityRegistration)} method yet. Commonly this is
   * expected to return at most one result.
   * </p>
   * 
   * @return the exchange registration entities
   */
  Iterable<ExchangeRegistrationEntity> listExchangeRegistrations();

  /**
   * Get the registration form for a given exchange.
   * 
   * @param exchange
   *        the exchange to get the registration form from
   * @param locale
   *        the desired locale of the form
   * @return the form
   */
  DerFacilityRegistrationForm getExchangeRegistrationForm(DerFacilityExchangeInfo exchange,
      Locale locale);

  /**
   * Register with an exchange.
   * 
   * @param exchange
   *        the exchange to register with
   * @param formData
   *        the registration form data to submit
   * @return the created registration entity
   */
  ExchangeRegistrationEntity registerWithExchange(DerFacilityExchangeInfo exchange,
      FormData formData);

  /**
   * Complete the exchange registration process.
   * 
   * <p>
   * Implementations must send a {@link ExchangeRegistrationEvent.ExchangeRegistrationCompleted}
   * event after processing the request.
   * </p>
   * 
   * @param request
   *        the registration success indicator
   * @return the registered exchange entity
   */
  ExchangeEntity completeExchangeRegistration(DerFacilityRegistration request);

}
