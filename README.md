#  openutils-deployment
 Openutils-deployment contains a few utility classes that allow switching between different configurations in an application.

 Openutils-deployment works by embedding configurations for different environments in the final application, and by
 choosing the correct configuration at runtime dependending on:
* server name
* webapp name (for web projects)
* web context init parameters
* system properties

 You can find a spring propertyconfigurer and a log4j servlet listener that follow this pattern.

 The following example is a configuration snippet for  `it.openutils.deployment.spring.EnvironmentPropertyConfigurer`:
 The `${env}` variable will be replaced at runtime with the name of the current server and any existing configuration file
 wil be loaded.

```
  <bean id="environmentProperties" class="it.openutils.deployment.spring.EnvironmentPropertyConfigurer">
    <property name="fileLocation"
      value="WEB-INF/config/${env}/environment.properties,
      WEB-INF/config/default/environment.properties,
      classpath:environment-${env}.properties,
      classpath:environment.properties" />
  </bean>
```


Released versions:
Check it at https://search.maven.org/search?q=g:net.sourceforge.openutils%20AND%20a:openutils-deployment&core=gav

