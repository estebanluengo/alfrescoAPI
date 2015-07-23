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
package info.estebanluengo.alfrescoAPI.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * A TestSuite to make a test using AlfrescoAPI
 * 
 * @author Esteban Luengo Simón
 * @version 1.0 11/May/2015
 */
@Suite.SuiteClasses({AlfrescoAPICRUDTest.class, AlfrescoAPIQueryTest.class})
@RunWith(Suite.class)
public class AlfrescoAPITestSuite {
    
}
