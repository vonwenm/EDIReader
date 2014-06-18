/*
 * Copyright 2005-2011 by BerryWorks Software, LLC. All rights reserved.
 *
 * This file is part of EDIReader. You may obtain a license for its use directly from
 * BerryWorks Software, and you may also choose to use this software under the terms of the
 * GPL version 3. Other products in the EDIReader software suite are available only by licensing
 * with BerryWorks. Only those files bearing the GPL statement below are available under the GPL.
 *
 * EDIReader is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * EDIReader is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EDIReader.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package com.berryworks.edireader;

import com.berryworks.edireader.tokenizer.Tokenizer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Determines and maintains state transitions for the segment looping structure
 * within a particular EDI document.
 * <p/>
 * An EDI parser delegates the job of detecting segment loop boundaries
 * to a PluginController. This allows the EDI parsers for ANSI and EDIFACT
 * to be fully consistent with their use of plugins and focus on the specifics of
 * the particular EDI standard.
 * <p/>
 * This base implementation of PluginController provides the normal
 * segment loop support based on LoopDescriptors in Plugins.
 * It is possible, however, to extend this behavior by subclassing
 * PluginController and Plugin. A ValidatingPlugin is one example,
 * which provides for certain EDI validation rules, beyond those applied by
 * normal EDIReader parsing, to be applied while a document is being parsed.
 * Another example is a FilteringPlugin, which allows a plugin to provide
 * custom logic to filter out certain LoopDescriptors based on run-time decisions.
 *
 * @see com.berryworks.edireader.Plugin
 * @see com.berryworks.edireader.plugin.LoopDescriptor
 * @see com.berryworks.edireader.validator.FilteringPluginController
 * @see com.berryworks.edireader.validator.ValidatingPluginController
 */
public class PluginController
{
  protected static boolean debug;

  /**
   * Creates a new instance of a PluginController, selecting a plugin based on the standard and type of document.
   *
   * @param standard
   * @param docType
   * @param tokenizer
   * @return instance
   */
  public static PluginController create(String standard, String docType, Tokenizer tokenizer)
  {
    return create(standard, docType, null, null, tokenizer);
  }

  /**
   * Creates a new instance of a PluginController, selecting a plugin based on the standard, the type of document,
   * and the version and release characteristics.
   * <p/>
   * This factory method delegates to the corresponding create method on a PluginControllerImpl,
   * if such a class is available. If not, then this factory method simply returns an instance of this
   * PluginController class, which supports the required interfaces but disables the entire plugin mechanism.
   *
   * @param standard
   * @param docType
   * @param docVersion
   * @param docRelease
   * @param tokenizer
   * @return instance
   */
  public static PluginController create(String standard,
                                        String docType,
                                        String docVersion,
                                        String docRelease,
                                        Tokenizer tokenizer)
  {
    try
    {
      Class controllerClass = Class.forName("com.berryworks.edireader.plugin.PluginControllerImpl");

      Method createMethod = controllerClass.getDeclaredMethod("create",
        String.class, String.class, String.class, String.class, Tokenizer.class);

      return (PluginController) createMethod.invoke(null,
        standard, docType, docVersion, docRelease, tokenizer);

    } catch (ClassNotFoundException ignored)
    {
    } catch (NoSuchMethodException ignored)
    {
    } catch (InvocationTargetException ignored)
    {
    } catch (IllegalAccessException ignored)
    {
    }

    return new PluginController();
  }

  /**
   * Compute a state transition that may have occurred as the result of the
   * presence of a particular segment type at this point in parsing the
   * document.
   *
   * @param segmentName type of segment encountered, for example: 837
   * @return true if there was a transition to a new loop, false otherwise
   * @throws com.berryworks.edireader.EDISyntaxException
   *          Description of the Exception
   */
  public boolean transition(String segmentName) throws EDISyntaxException
  {
    return false;
  }

  /**
   * Return the name of a loop that was entered as the result of the most
   * recent transition.
   *
   * @return name of the entered loop, or null if no loop was entered
   */
  public String getLoopEntered()
  {
    return null;
  }

  /**
   * Get the number of loops that were closed as the result of the most recent
   * state transition. Re-entering the implicit outer loop does not count as a
   * loop closing.
   *
   * @return Description of the Return Value
   */
  public int closedCount()
  {
    return 0;
  }

  /**
   * Get the nesting level of the current loop.
   *
   * @return Description of the Return Value
   */
  public int getNestingLevel()
  {
    return 0;
  }

  /**
   * Returns true if this controller is currently enabled.
   *
   * @return
   */
  public boolean isEnabled()
  {
    return false;
  }

  /**
   * Returns the document name associated with the plugin.
   *
   * @return
   */
  public String getDocumentName()
  {
    return null;
  }

  /**
   * Returns the Plugin used by this PluginController.
   *
   * @return Plugin
   */
  public Plugin getPlugin()
  {
    return null;
  }

  /**
   * Shorthand for EDIReader.trace(String)
   *
   * @param text message to appear in trace
   */
  protected static void trace(String text)
  {
    EDIAbstractReader.trace(text);
  }

  /**
   * Returns true if the most recent loop transition was to resume an outer loop.
   *
   * @return
   */
  public boolean isResumed()
  {
    return false;
  }

  /**
   * Returns the class name of the most recently loaded plugin.
   *
   * @return
   */
  public String lastPluginLoaded()
  {
    return null;
  }

  /**
   * Sets debugging on or off.
   *
   * @param d
   */
  public static void setDebug(boolean d)
  {
    if (d)
      if (debug)
        PluginController.trace("PluginController: debug already on");
      else
      {
        PluginController.trace("PluginController: debug turn on");
        debug = true;
      }
    else if (debug)
    {
      PluginController.trace("PluginController: debug turn off");
      debug = false;
    }
    // If it's already off, leave it off but keep quiet about it
  }
}
