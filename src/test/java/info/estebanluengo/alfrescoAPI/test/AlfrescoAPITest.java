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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.SecondaryType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import info.estebanluengo.alfrescoAPI.AlfrescoAPI;
import info.estebanluengo.alfrescoAPI.test.conf.TestProperties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base test class for the AlfrescoAPI Test Suite
 * 
 * @author Esteban Luengo Simón
 * @version 1.0 11/May/2015
 */
public abstract class AlfrescoAPITest {
    private static final Logger logger = LogManager.getLogger();
    public static String DOC_TYPE = "D:sc:whitepaper";
    public static String PDF_MIME_TYPE = "application/pdf";
    public static String PLAINTEXT_MIME_TYPE = "plain/text";
    public static String ASSOCIATION_TYPE = "R:sc:relatedDocuments";
    
    @Autowired
    protected TestProperties testProperties;
    //Session object to access the Alfreco server
    protected static Session session;
    
    protected static String username;
            
    @Before
    public void setUp() {
        //not needed if we use @ContextConfiguration annotation
//        AbstractApplicationContext  context = new AnnotationConfigApplicationContext(AppConfig.class);
    }
    
    /**
     * Creates a new Session object only y the static session member object is null
     * 
     * @return a Session object that is connected to the Alfresco Server
     */
    protected Session createSessionIfNeeded(){
        if (session == null){
            logger.debug("Session is null. Creating session");
            session = AlfrescoAPI.createSession(testProperties.getUsername(), 
                    testProperties.getPassword(), 
                    testProperties.getServer());
            username = testProperties.getUsername();
        
        }
        return session;
    }
    
    /**
     * Creates a new Session object and set into the static session member object class
     * 
     * @return a Session object that is connected to the Alfresco Server 
     */
    protected Session createNewSession(){
        session = AlfrescoAPI.createSession(testProperties.getUsername(), 
                    testProperties.getPassword(), 
                    testProperties.getServer());
        username = testProperties.getUsername();
        return session;
    }
    
    /**
     * Gets a folder object from Alfresco server. 
     * 
     * @param parentFolder a Folder object where the folder to retrieve exist
     * @param folderName the folder name
     * 
     * @return a Folder object that represent the folder
     */
    protected Folder getFolder(Folder parentFolder, String folderName){
        Folder aFolder = AlfrescoAPI.getFolderByName(session, parentFolder, folderName);        
        return aFolder;
    }
    
    /**
     * Gets a folder object from Alfresco server. 
     * 
     * @param folderName the folder name
     * @return a Folder object that represent the folder
     */
    protected Folder getFolder(String folderName){
        Folder aFolder = AlfrescoAPI.getFolderByName(session, folderName);        
        return aFolder;
    }
    
    /**
     * Returns a byte[] that contain the data of the file located int src/test/resources directory
     * 
     * @return a byte[] that contain the file data     
     * @throws IOException if there is not any file in the path
     */
    protected byte[] getFile() throws IOException{
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream input = classLoader.getResourceAsStream(testProperties.getFilename());
        return org.apache.commons.io.IOUtils.toByteArray(input);
    }
    
    /**
     * Gets a document from Alfresco server by Id
     * 
     * @param id a String that represent the document Id
     * @return a Document object
     */
    protected Document getDocument(String id){
        return AlfrescoAPI.getDocument(session, id);
    }
    
    /**
     * Gets a file name that is composed by the filename of the property mapped in config.properties file
     * plus a System.currentTimeMillis info
     * 
     * @return a String that represent the filename
     */
    protected String getFileName(){
        String timeStamp = Long.toString(System.currentTimeMillis());
        return timeStamp + testProperties.getFilename();
    }
    
    /**
     * Makes a document properties with two aspects P:sc:webable and P:cm:author and the filename
     * 
     * @param fileName a String that represent the file name for the document
     * @param author a String that represent the author property
     * @param active a boolean that represent if the document is active or not
     * @return a Map<String, Object> with the properties
     */
    protected Map<String, Object> getDocProperties(String fileName, String author, boolean active){
        Map<String, Object> docProps = new HashMap<>();
        List<Object> aspects = new ArrayList<>();
        aspects.add("P:sc:webable");
        aspects.add("P:cm:author");
        docProps.put("cmis:secondaryObjectTypeIds", aspects);
        docProps.put(PropertyIds.NAME, fileName);
        docProps.put("sc:isActive", active);
        docProps.put("cm:author", author);
        
        return docProps;
    }
    
    /**
     * Checks if the new document:
     * <ol>
     * <li>Its not null</li>
     * <li>Exist in the server with the same Id of doc.getId()</li>
     * <li>Its version is 1.0</li>
     * <li>Its name is equal to fileName argument</li>
     * <li>Its parent folder name is equal to the folderName argument</li>
     * <li>Its content lenght is equal to the contentFileLength argument</li>
     * <li>Its mime type is equal to the mimeType argument</li>
     * </ol>
     * @param doc a Document object to check
     * @param fileName a String that represent the file name
     * @param folderName a String that represent the folder name to check
     * @param contentFileLength a int that represent the document content length
     * @param mimeType a String that represent the mime type
     */
    protected void checkNewDocument(Document doc, String fileName, String folderName, int contentFileLength, String mimeType){
        assertNotNull(doc);
        assertEquals(fileName, doc.getName());
        List<Folder> paths = doc.getParents();
        assertEquals(folderName, paths.get(0).getName());        
        Document docCreated = getDocument(doc.getId());
        assertEquals(docCreated.getId(), doc.getId());
        assertEquals(contentFileLength, doc.getContentStreamLength());
        assertEquals(mimeType, doc.getContentStreamMimeType());
        assertEquals(doc.getVersionLabel(), "1.0");
    }
    
    /**
     * Checks if the document:
     * <ol>
     * <li>Has a webable aspect</li>
     * <li>Has a author aspect</li>
     * <li>The document cm:author property is equal to the author argument</li>
     * <li>The document sc:isActive property is equal to the active arguement</li>
     * </ol>
     * @param doc a Document object to check
     * @param author a String that represent the author property to check
     * @param active a boolean that represent the active property to check
     */
    protected void checkProperties(Document doc, String author, boolean active){
        List<SecondaryType> secTypes = doc.getSecondaryTypes();        
        assertTrue(containsSecondaryType(secTypes, "webable"));
        assertTrue(containsSecondaryType(secTypes, "author"));
        assertEquals(doc.getPropertyValue("cm:author"), author);
        assertEquals((Boolean)doc.getPropertyValue("sc:isActive"), active);
    }
    
    /**
     * Checks if the secTypes arguemnt contains the secondaryType argument
     * 
     * @param secTypes a List<org.apache.chemistry.opencmis.client.api.SecondaryType> 
     * @param secondaryType a String that represent the secondaryType to find
     * 
     * @return true if the secTypes contains a secondaryType data
     */
    protected boolean containsSecondaryType(List<SecondaryType> secTypes, String secondaryType){
        for (SecondaryType st: secTypes){
            if (st.getLocalName().equals(secondaryType)){
                return true;
            }                
        }
        return false;
    }
    
    /**
     * Checks if the docList argument contains an object with Id equals to the id arguement
     * 
     * @param docList a List<org.apache.chemistry.opencmis.client.api.CmisObject> where the method searches for the object
     * @param id a String that represent the Id to find
     * 
     * @return true if the list contains an object with this id
     */
    protected boolean containsDocument(List<CmisObject> docList, String id){
        for (CmisObject doc: docList){
            if (doc.getId().equals(id)){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Deletes a document from the server
     * 
     * @param docId a String that represent the document Id
     */
    protected void deleteDocument(String docId){        
        AlfrescoAPI.deleteDocument(session, docId, true);
        logger.debug("document removed");
    }    
    
    /**
     * Deletes a document from the server
     * 
     * @param doc a Document object to delete
     */
    protected void deleteDocument(Document doc){
        if (doc != null){
            deleteDocument(doc.getId());            
        }
    }
    
    /**
     * Deletes a folder from the server. The method will fail if the folder 
     * contains subfolders
     * 
     * @param folder a Folder object to delete
     */
    protected void deleteFolder(Folder folder){
        if (folder != null){
            AlfrescoAPI.deleteFolder(session, folder);
            logger.debug("folder removed");
        }
    }
    
    /**
     * Deletes a folder from the server and all the subfolders 
     * 
     * @param folder a Folder object to delete
     * @param allVersions a boolean to indicate if we want to delete all the folder versions 
     */
    protected void deleteFolder(Folder folder, boolean allVersions){
        if (folder != null){
            AlfrescoAPI.deleteFolder(session, folder, allVersions);
            logger.debug("folder and all children removed");
        }
    }
    
    /**
     * Logs all document properties
     * @param doc a Document object
     */
    protected void dumpProperties(Document doc){
        List<Property<?>> props = doc.getProperties();
        for (Property<?> p : props) {
            logger.debug(p.getDefinition().getDisplayName() + "=" + p.getValuesAsString());
        }
    }
}