/**
 * Copyright 2015 Esteban Luengo Simón
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
package info.estebanluengo.alfrescoAPI.test.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * This class encapsulates the properties from config.properties file. 
 * <br>Read more at <a href="http://www.javacodegeeks.com/2015/04/spring-from-the-trenches-injecting-property-values-into-configuration-beans.html">this article</a>
 * 
 * @author Esteban Luengo Simón
 * @version 1.0 12/05/2015
 * 
 */
@Component
public class TestProperties {
    
    private final String username;
    private final String password;
    private final String server;
    private final String filename;
    
    //Environment contains all the properties that Spring will read from config.properties file    
    @Autowired
    public TestProperties(Environment environment){
        username = environment.getProperty("username");
        password = environment.getProperty("password");
        server = environment.getProperty("server");
        filename = environment.getProperty("filename");
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getServer() {
        return server;
    }

    public String getFilename() {
        return filename;
    }
       
}