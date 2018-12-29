/**
 *
 * openutils deployment tools (http://www.openmindlab.com/lab/products/deployment.html)
 * Copyright(C) 2007-2019, Openmind S.r.l. http://www.openmindonline.it
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package it.openutils.deployment.spring;

import org.springframework.stereotype.Component;


/**
 * @author fgiust
 * @version $Id$
 */
@Component
public class SampleBean
{

    @Property("intProperty")
    private Integer intProperty;

    @Property("stringProperty")
    private String stringProperty;

    @Property("nested")
    private String nestedProperty;

    /**
     * Returns the intProperty.
     * @return the intProperty
     */
    public Integer getIntProperty()
    {
        return intProperty;
    }

    /**
     * Returns the stringProperty.
     * @return the stringProperty
     */
    public String getStringProperty()
    {
        return stringProperty;
    }

    /**
     * Returns the nestedProperty.
     * @return the nestedProperty
     */
    public String getNestedProperty()
    {
        return nestedProperty;
    }

}
