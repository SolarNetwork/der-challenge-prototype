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

package net.solarnetwork.esi.cli;

import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Basic support for a service using a {@link MessageSource}.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseMessageSourceSupport {

  /** The message source. */
  protected MessageSource messageSource;

  /**
   * Constructor.
   * 
   * <p>
   * A default {@link ResourceBundleMessageSource} based on the instance's class name will be
   * created.
   * </p>
   */
  public BaseMessageSourceSupport() {
    super();
    ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
    ms.setBasenames(BaseMessageSourceSupport.class.getName(), getClass().getName());
    setMessageSource(ms);
  }

  /**
   * Constructor.
   */
  public BaseMessageSourceSupport(MessageSource messageSource) {
    super();
    setMessageSource(messageSource);
  }

  /**
   * Configure the message source.
   * 
   * @param messageSource
   *        the message source to set
   */
  public void setMessageSource(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

}
