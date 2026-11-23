/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.activitypub.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Base class of the test replacements of the macros of the discussions application: displays the value of the
 * parameters in the attributes of a {@code div} of the class {@code test-macro}, so that the tests can check them.
 *
 * @version $Id$
 */
public abstract class AbstractTestMacro extends AbstractMacro<TestMacroParameters>
{
    /**
     * @param name the name of the macro
     */
    protected AbstractTestMacro(String name)
    {
        super(name, "Test replacement of the " + name + " macro", TestMacroParameters.class);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    @Override
    public List<Block> execute(TestMacroParameters parameters, String content, MacroTransformationContext context)
    {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("class", "test-macro");
        attributes.put("data-reference", parameters.getReference());
        attributes.put("data-namespace", parameters.getNamespace());
        attributes.put("data-type", parameters.getType());
        attributes.put("data-list-id", parameters.getListId());
        attributes.put("data-link-template", parameters.getLinkTemplate());
        return Collections.singletonList(new GroupBlock(attributes));
    }
}
