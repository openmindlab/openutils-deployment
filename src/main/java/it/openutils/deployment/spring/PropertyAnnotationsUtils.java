/*
 * Copyright Openmind http://www.openmindonline.it
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.openutils.deployment.spring;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.util.ReflectionUtils;


/**
 * Utility class for doing property replacement in fields.
 * @author fgiust
 * @version $Id$
 */
public final class PropertyAnnotationsUtils
{

    private static SimpleTypeConverter typeConverter = new SimpleTypeConverter();

    private PropertyAnnotationsUtils()
    {
        // don't instantiate
    }

    public static void autowireProperties(final Object bean, final Properties properties)
    {
        ReflectionUtils.doWithFields(bean.getClass(), new ReflectionUtils.FieldCallback()
        {

            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException
            {
                Property annotation = field.getAnnotation(Property.class);
                if (annotation != null)
                {
                    if (Modifier.isStatic(field.getModifiers()))
                    {
                        throw new IllegalStateException(
                            "PropertyAutowired annotation is not supported on static fields");
                    }

                    Object strValue = properties.get(annotation.value());

                    if (strValue != null)
                    {
                        Object value = typeConverter.convertIfNecessary(strValue, field.getType());
                        ReflectionUtils.makeAccessible(field);
                        field.set(bean, value);
                    }
                }
            }
        });
    }
}
