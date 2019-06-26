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

package net.solarnetwork.esi.simple.fac.impl;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import com.github.fonimus.ssh.shell.PromptColor;

/**
 * Utilities for the CLI shell.
 * 
 * @author matt
 * @version 1.0
 */
public final class ShellUtils {

  /**
   * Color a bold message.
   *
   * @param message
   *        message to return
   * @return colored message
   */
  public static String getBold(String message) {
    return new AttributedStringBuilder().append(message, AttributedStyle.BOLD).toAnsi();
  }

  /**
   * Color a bold message with given color.
   *
   * @param message
   *        message to return
   * @param color
   *        color to print
   * @return colored message
   */
  public static String getBoldColored(String message, PromptColor color) {
    return new AttributedStringBuilder()
        .append(message, AttributedStyle.BOLD.foreground(color.toJlineAttributedStyle())).toAnsi();
  }

  /**
   * Color a faint message.
   *
   * @param message
   *        message to return
   * @return colored message
   */
  public static String getFaint(String message) {
    return new AttributedStringBuilder().append(message, AttributedStyle.DEFAULT.faint()).toAnsi();
  }

}
