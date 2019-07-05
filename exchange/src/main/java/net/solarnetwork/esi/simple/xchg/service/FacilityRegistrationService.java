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

package net.solarnetwork.esi.simple.xchg.service;

import java.util.concurrent.Future;

import org.springframework.scheduling.annotation.Async;

import net.solarnetwork.esi.domain.DerFacilityRegistrationFormData;
import net.solarnetwork.esi.simple.xchg.domain.FacilityEntity;
import net.solarnetwork.esi.simple.xchg.domain.FacilityRegistrationEntity;

/**
 * API for a facility registration service.
 * 
 * @author matt
 * @version 1.0
 */
public interface FacilityRegistrationService {

  /** The form field key for the Utility Interconnection Customer Identifier value. */
  String FORM_KEY_UICI = "uici";

  /** The form field key for the cutomer's ID value. */
  String FORM_KEY_CUSTOMER_ID = "cust-id";

  /** The form field key for the cutomer's surname value. */
  String FORM_KEY_CUSTOMER_SURNAME = "cust-surname";

  /**
   * Submit a facility registration form.
   * 
   * @param request
   *        the registration request
   * @return the facility registration
   * @throws IllegalArgumentException
   *         if any validation errors occur
   */
  public FacilityRegistrationEntity submitDerFacilityRegistrationForm(
      DerFacilityRegistrationFormData request);

  /**
   * Process a facility registration.
   * 
   * @param registration
   *        the facility registration
   * @return the approved facility
   */
  @Async
  Future<FacilityEntity> processFacilityRegistration(FacilityRegistrationEntity registration);

}
