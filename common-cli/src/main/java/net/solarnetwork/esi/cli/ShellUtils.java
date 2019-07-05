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

import org.davidmoten.text.utils.WordWrap;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import com.github.fonimus.ssh.shell.PromptColor;
import com.github.fonimus.ssh.shell.SshContext;

/**
 * Utilities for the CLI shell.
 * 
 * @author matt
 * @version 1.0
 */
public final class ShellUtils {

  /** A default maximum width of shell output, i.e. for wrapping. */
  public static final int SHELL_MAX_COLS = 80;

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

  /**
   * Broadcast a message to all registered SSH shells.
   * 
   * @param message
   *        the message to broadcast
   */
  public static void wall(String message) {
    for (SshContext ctx : TrackingSshShellCommandFactory.sshContexts()) {
      ctx.getTerminal().writer().println(message);
      ctx.getTerminal().flush();
    }
  }

  /**
   * Wrap a string to a maximum character column width.
   * 
   * @param message
   *        the message to wrap
   * @param maxColumns
   *        the maximum number of characters wide to wrap the text at
   * @return the message with newline characters inserted where needed to wrap the text to at most
   *         {@code maxColumns} characters wide
   */
  public static String wrap(CharSequence message, int maxColumns) {
    return WordWrap.from(message).maxWidth(maxColumns).wrap();
  }

  /**
   * Wrap a string to the default character column width.
   * 
   * @param message
   *        the message to wrap
   * @return the message with newline characters inserted where needed to wrap the text to at most
   *         {@link #SHELL_MAX_COLS} characters wide
   */
  public static String wrap(CharSequence message) {
    return wrap(message, SHELL_MAX_COLS);
  }

}
