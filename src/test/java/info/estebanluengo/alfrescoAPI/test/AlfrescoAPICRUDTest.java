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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.SecondaryType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.commons.io.IOUtils;
import static org.junit.Assert.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import info.estebanluengo.alfrescoAPI.AlfrescoAPI;
import info.estebanluengo.alfrescoAPI.test.conf.AppConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This Test class makes different CRUD operations using AlfrescoAPI class
 * 
 * @author Esteban Luengo Simón
 * @version 1.0 11/May/2015
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class AlfrescoAPICRUDTest extends AlfrescoAPITest
{
    private static final Logger logger = LogManager.getLogger();                   
    
    @AfterClass
    public static void cleanRepository(){        
        Folder parentFolder = AlfrescoAPI.getFolderByName(session, username);
        AlfrescoAPI.deleteChildren(session, parentFolder, true);
    }
    
    @Test
    public void createSession(){
        logger.debug("Init createSession test");
        createSessionIfNeeded();
        assertNotNull(session);        
    }
    
    @Test
    public void createFolder(){
        logger.debug("Init createFolder test");
        createSessionIfNeeded();
        Folder parentFolder =  getFolder(testProperties.getUsername());        
        String timeStamp = Long.toString(System.currentTimeMillis());
        String folderName = "newFolder("+timeStamp+")";
        Folder newFolder = null;
        try{
            newFolder = AlfrescoAPI.createFolder(session, parentFolder, folderName);                
            assertNotNull(newFolder);
            assertEquals(newFolder.getFolderParent().getName(), parentFolder.getName());
            assertEquals(newFolder.getName(), folderName);
        }finally{
            logger.debug("removing the new folder");
            deleteFolder(newFolder);
        }        
    }
    
    @Test
    public void createAndDeleteFolders(){
        logger.debug("Init createAndDeleteFolders test");
        createSessionIfNeeded();
        Folder parentFolder =  getFolder(testProperties.getUsername());        
        logger.debug(parentFolder.getPath());
        String folderName = "folder1/folder2/folder3";
        Folder newFolder;
        try{
            newFolder = AlfrescoAPI.createFolders(session, parentFolder, folderName);                
            logger.debug(newFolder.getPath());
            assertNotNull(newFolder);
            assertEquals(newFolder.getName(), "folder3");
            assertEquals(newFolder.getPath(),parentFolder.getPath()+"/"+ folderName);
        }finally{
            logger.debug("Deleting the new folder");
//            deleteFolder(parentFolder); //CmisConstraintException 
            deleteFolder(AlfrescoAPI.getFolderByName(session, parentFolder, "folder1"), true);
            Folder folderNotExists = AlfrescoAPI.getFolderByName(session, parentFolder, "folder1");
            assertNull(folderNotExists);        
        }                
    }
    
    @Test
    public void deleteChildren() throws IOException{
        logger.debug("Init createAndDeleteFolders test");
        createSessionIfNeeded();
        Folder parentFolder =  getFolder(testProperties.getUsername());                
        String folderName = "aFolder";
        String fileName = getFileName();
        byte[] contentFile = getFile();
        Folder newFolder;
        try{
            newFolder = AlfrescoAPI.createFolder(session, parentFolder, folderName+"1");
            AlfrescoAPI.createFolder(session, parentFolder, folderName+"2");    
            AlfrescoAPI.createDocument(session, newFolder, fileName, contentFile, PDF_MIME_TYPE);                
            AlfrescoAPI.createDocument(session, newFolder, "_"+fileName, contentFile, PDF_MIME_TYPE);
            AlfrescoAPI.createDocument(session, parentFolder, "_"+fileName, contentFile, PDF_MIME_TYPE);
            ItemIterable<CmisObject> childrenParentFolder = parentFolder.getChildren();
            assertNotNull(childrenParentFolder);
            logger.debug("Children size:"+childrenParentFolder.getTotalNumItems());
            assertTrue(childrenParentFolder.getTotalNumItems() == 3);
            ItemIterable<CmisObject> childrenNewFolder = newFolder.getChildren();
            assertNotNull(childrenNewFolder);
            logger.debug("Children size:"+childrenNewFolder.getTotalNumItems());
            assertTrue(childrenNewFolder.getTotalNumItems() == 2);
            
            AlfrescoAPI.deleteChildren(session, parentFolder, true);
            childrenParentFolder = parentFolder.getChildren();
            assertNotNull(childrenParentFolder);
            logger.debug("Children size:"+childrenParentFolder.getTotalNumItems());
            assertTrue(childrenParentFolder.getTotalNumItems() == 0);
        }finally{
            AlfrescoAPI.deleteChildren(session, parentFolder, true);
        }
    }
    
    @Test(expected = CmisContentAlreadyExistsException.class)
    public void createSameFolder(){
        logger.debug("Init createSameFolder test");
        createSessionIfNeeded();
        Folder parentFolder =  getFolder(testProperties.getUsername());        
        String timeStamp = Long.toString(System.currentTimeMillis());
        String folderName = "newFolder("+timeStamp+")";
        Folder newFolder = null;        
        try{
            newFolder = AlfrescoAPI.createFolder(session, parentFolder, folderName);                
            AlfrescoAPI.createFolder(session, parentFolder, folderName);
        }finally{
            logger.debug("removing the new folder");
            deleteFolder(newFolder);
        }
    }
        
    @Test
    public void getFolder(){
        logger.debug("Init getFolder test");
        createSessionIfNeeded();
        String folderName = testProperties.getUsername();
        Folder aFolder = getFolder(folderName);        
        assertNotNull(aFolder);
        assertEquals(aFolder.getName(), folderName);               
    }
    
    @Test
    public void getFolderFromParent(){
        logger.debug("Init getFolderFromParent test");
        createSessionIfNeeded();
        String folderName = "aFolder";
        Folder parentFolder = getFolder(testProperties.getUsername());        
        Folder aFolder = null;
        try{
            AlfrescoAPI.createFolder(session, parentFolder, folderName);
            aFolder = AlfrescoAPI.getFolderByName(session, parentFolder, folderName);        
            assertNotNull(aFolder);
            assertEquals(aFolder.getName(), folderName);        
            assertEquals(aFolder.getPath(), parentFolder.getPath()+"/"+aFolder.getName());
        }finally{
            deleteFolder(aFolder);
        }
    }
    
    @Test
    public void getFolders(){
        logger.debug("Init getFolders test");
        createSessionIfNeeded();
        String folderName = "aFolder";
        Folder parentFolder = getFolder(testProperties.getUsername());        
        List<Folder> folderList = null;
        try{
            AlfrescoAPI.createFolder(session, parentFolder, folderName+"1");
            AlfrescoAPI.createFolder(session, parentFolder, folderName+"2");
            folderList = AlfrescoAPI.getFolders(session, parentFolder, true);            
            assertNotNull(folderList);
            logger.debug("FolderList size:"+folderList.size());
            assertTrue(folderList.size() == 2);
            int i = 1;
            for (Folder folder: folderList){
                assertEquals(folder.getName(), folderName+(i++));
            }
        }finally{
            if (folderList != null){
                for (Folder folder: folderList){
                    deleteFolder(folder, true);
                }
            }
        }
    }
            
    @Test
    public void createAndGetDocument() throws IOException{
        logger.debug("Init createDocument test");
        createSessionIfNeeded();
        String fileName = getFileName();
        String folderName = testProperties.getUsername();
        byte[] contentFile = getFile();
        Document doc = null;
        try{
            doc = AlfrescoAPI.createDocument(session, getFolder(folderName), fileName, contentFile, PDF_MIME_TYPE);                
            checkNewDocument(doc, fileName, folderName, contentFile.length, PDF_MIME_TYPE);
        }finally{
            deleteDocument(doc);
        }
    }
    
    @Test(expected = CmisContentAlreadyExistsException.class)
    public void createSameDocument() throws IOException{
        logger.debug("Init createSameDocument test");
        createSessionIfNeeded();
        String fileName = getFileName();
        String folderName = testProperties.getUsername();
        byte[] contentFile = getFile();
        Document doc = null;
        try{
            doc = AlfrescoAPI.createDocument(session, getFolder(folderName), fileName, contentFile, PDF_MIME_TYPE);                
            AlfrescoAPI.createDocument(session, getFolder(folderName), fileName, contentFile, PDF_MIME_TYPE);                             
        }finally{
            deleteDocument(doc);
        }
    }
    
    @Test
    public void createDocumentWithMetaData() throws IOException{
        logger.debug("Init createDocumentWithMetaData test");
        createSessionIfNeeded();
        String fileName = getFileName();
        String author = testProperties.getUsername()+"_author";
        boolean active = true;
        Map<String, Object> docProps = getDocProperties(fileName, author, active);        
        String folderName = testProperties.getUsername();
        byte[] contentFile = getFile();
        Document doc = null;
        try{
            doc = AlfrescoAPI.createDocument(session, getFolder(folderName), fileName, contentFile, PDF_MIME_TYPE, DOC_TYPE, docProps);                        
            checkNewDocument(doc, fileName, folderName, contentFile.length, PDF_MIME_TYPE);                
            List<SecondaryType> secTypes = doc.getSecondaryTypes();        
            assertTrue(containsSecondaryType(secTypes, "webable"));
            assertTrue(containsSecondaryType(secTypes, "author"));
            assertEquals(doc.getPropertyValue("cm:author"), author);
            assertEquals((Boolean)doc.getPropertyValue("sc:isActive"), active);
        }finally{
            deleteDocument(doc);
        }
    }
    
    @Test
    public void relatedDocumentsWithCustomModelTypeRelationShip() throws IOException{
        logger.debug("Init relatedDocuments test");
        createSessionIfNeeded();
        String fileName = getFileName();
        String author = testProperties.getUsername()+"_author";
        boolean active = true;
        Map<String, Object> docProps = getDocProperties(fileName, author, active);
        String folderName = testProperties.getUsername();
        byte[] contentFile = getFile();
        Document doc1 = null;
        Document doc2 = null;
        Folder folder = getFolder(folderName);
        try{
            doc1 = AlfrescoAPI.createDocument(session, folder, fileName, contentFile, PDF_MIME_TYPE, DOC_TYPE, docProps);
            doc2 = AlfrescoAPI.createDocument(session, folder, "_"+ fileName, contentFile, PDF_MIME_TYPE, DOC_TYPE, docProps);
            AlfrescoAPI.relatedDocuments(session, doc1, doc2, ASSOCIATION_TYPE);
            Document docCreated = AlfrescoAPI.getDocumentWithRelationShips(session, doc1.getId(), false);
            assertNotNull(docCreated);        
            List<Relationship> relationShips = docCreated.getRelationships();
            assertNotNull(relationShips);
            for (Relationship rs: relationShips){
                assertEquals(doc1.getId(), rs.getSource().getId());
                assertEquals(doc2.getId(), rs.getTarget().getId());
            }
        }
        finally{             
            deleteDocument(doc1);
            deleteDocument(doc2);            
        }
    }
    
    @Test
    public void relatedDocuments() throws IOException{
        logger.debug("Init relatedDocuments test");
        createSessionIfNeeded();
        String fileName = getFileName();
        String folderName = testProperties.getUsername();
        byte[] contentFile = getFile();
        Document doc1 = null;
        Document doc2 = null;
        Folder folder = getFolder(folderName);
        try{
            doc1 = AlfrescoAPI.createDocument(session, folder, fileName, contentFile, PDF_MIME_TYPE, AlfrescoAPI.CUSTOM_DOCUMENT_TYPE,null);
            doc2 = AlfrescoAPI.createDocument(session, folder, "_"+ fileName, contentFile, PDF_MIME_TYPE, AlfrescoAPI.CUSTOM_DOCUMENT_TYPE,null);
            AlfrescoAPI.relatedDocuments(session, doc1, doc2, AlfrescoAPI.CUSTOM_ASSOCIATION);
            Document docCreated = AlfrescoAPI.getDocumentWithRelationShips(session, doc1.getId(), false);
            assertNotNull(docCreated);        
            List<Relationship> relationShips = docCreated.getRelationships();
            assertNotNull(relationShips);
            for (Relationship rs: relationShips){
                assertEquals(doc1.getId(), rs.getSource().getId());
                assertEquals(doc2.getId(), rs.getTarget().getId());
            }
        }
        finally{             
            deleteDocument(doc1);
            deleteDocument(doc2);            
        }
    }
    
    @Test
    public void updateDocument() throws IOException{
        logger.debug("Init updateDocument test");
        createSessionIfNeeded();
        String fileName = getFileName();
        String folderName = testProperties.getUsername();
        String author = testProperties.getUsername()+"_author";
        boolean active = true;
        byte[] contentFile = getFile();
        Map<String, Object> docProps = getDocProperties(fileName, author, active);
        Document doc = null;
        Document updatedDocument;
        try{
            doc = AlfrescoAPI.createDocument(session, getFolder(folderName), fileName, contentFile, PDF_MIME_TYPE, DOC_TYPE, docProps);    
            logger.debug("version:"+doc.getVersionLabel()+" id:"+doc.getId());
            checkNewDocument(doc, fileName, folderName, contentFile.length, PDF_MIME_TYPE);            
            Map<String, Object> docProps2 = new HashMap<>();        
            //Not include aspects or a Exception "Use CheckOutCheckInservice to manipulate working copies" will be thown
            docProps2.put("sc:isActive", !active);
            docProps2.put("cm:author", "_"+author);
            byte[] newContentFile = "Nuevo documento de texto".getBytes();
            doc = getDocument(doc.getId());
            updatedDocument = AlfrescoAPI.updateDocument(session, doc, newContentFile, PLAINTEXT_MIME_TYPE, docProps2, true, "a major change");          
            //another chance. Maybe the document is not ready yet on the server
            if (updatedDocument == null){
                updatedDocument = AlfrescoAPI.updateDocument(session, doc, newContentFile, PLAINTEXT_MIME_TYPE, docProps2, true, "a major change");                       
            }
            assertNotNull(updatedDocument);
            logger.debug("version:"+updatedDocument.getVersionLabel()+" id:"+updatedDocument.getId());
            assertEquals(updatedDocument.getVersionLabel(),"2.0");
            checkProperties(updatedDocument, "_"+author, !active);        
        }finally{
            deleteDocument(doc);
        }
    }
    
    @Test
    public void updateDocumentProperties() throws IOException{
        logger.debug("Init createDocumentWithMetaData test");
        createSessionIfNeeded();
        String fileName = getFileName();
        String author = testProperties.getUsername()+"_author";
        boolean active = true;
        Map<String, Object> docProps = getDocProperties(fileName, author, active);        
        String folderName = testProperties.getUsername();
        byte[] contentFile = getFile();
        Document doc = null;
        try{
            doc = AlfrescoAPI.createDocument(session, getFolder(folderName), fileName, contentFile, PDF_MIME_TYPE, DOC_TYPE, docProps);                        
            checkNewDocument(doc, fileName, folderName, contentFile.length, PDF_MIME_TYPE);                
            checkProperties(doc, author, active);            
            
            Map<String, Object> docUpdatedProps = getDocProperties(fileName, "_"+author, !active); 
            Document updatedDocument = AlfrescoAPI.updateDocumentProperties(session, doc, docUpdatedProps);
            checkProperties(updatedDocument, "_"+author, !active);            
        }finally{
            deleteDocument(doc);
        }
    }
    
    @Test
    public void getDocumentContent() throws IOException{
        logger.debug("Init getDocumentContent test");
        createSessionIfNeeded();
        String fileName = getFileName();
        String folderName = testProperties.getUsername();
        byte[] contentFile = getFile();
        Document doc = null;
        try{
            doc = AlfrescoAPI.createDocument(session, getFolder(folderName), fileName, contentFile, PDF_MIME_TYPE);                
            byte[] contentDoc = AlfrescoAPI.getDocumentContent(session, doc.getId());
            assertNotNull(contentDoc);
            assertTrue(contentDoc.length == contentFile.length);
            assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(contentFile), new ByteArrayInputStream(contentDoc)));
        }finally{
            deleteDocument(doc);
        }
    }
    
    @Test(expected = CmisObjectNotFoundException.class)
    public void getDocumentNotExist() throws IOException{
        logger.debug("Init getDocumentNotExist test");
        createSessionIfNeeded();        
        AlfrescoAPI.getDocument(session, "document_"+Long.toString(System.currentTimeMillis()));        
    }
 
    @Test
    public void createDocumentVersion() throws IOException{
        logger.debug("Init updateDocument test");
        createSessionIfNeeded();
        String fileName = getFileName();
        String folderName = testProperties.getUsername();
        String author = testProperties.getUsername()+"_author";
        boolean active = true;
        byte[] contentFile = getFile();
        Map<String, Object> docProps = getDocProperties(fileName, author, active);
        Document doc = null;
        Document updatedDocument;
        try{
            doc = AlfrescoAPI.createDocument(session, getFolder(folderName), fileName, contentFile, PDF_MIME_TYPE, DOC_TYPE, docProps);                
            logger.debug("id:"+doc.getId());
            logger.debug("Version:"+doc.getVersionLabel());
            Map<String, Object> docProps2 = new HashMap<>();                    
            docProps2.put("sc:isActive", !active);
            docProps2.put("cm:author", "_"+author);
            byte[] newContentFile = "Nuevo documento de texto".getBytes();
            doc = getDocument(doc.getId());
            updatedDocument = AlfrescoAPI.updateDocument(session, doc, newContentFile, PLAINTEXT_MIME_TYPE, docProps2, true, "a major change");                       
            if (updatedDocument == null){
                updatedDocument = AlfrescoAPI.updateDocument(session, doc, newContentFile, PLAINTEXT_MIME_TYPE, docProps2, true, "a major change");                       
            }
            assertNotNull(updatedDocument);
            logger.debug("id:"+updatedDocument.getId());
            List<Document> allVersions = AlfrescoAPI.getAllVersionsOfDocument(session, doc.getId());
            assertNotNull(allVersions);
            assertTrue(allVersions.size() == 2);
            boolean version1 = false;
            boolean version2 = false;
            for (Document d: allVersions){
                if (d.getVersionLabel().equals("1.0")){
                    version1 = true;
                }
                if (d.getVersionLabel().equals("2.0")){
                    version2 = true;
                }
            }
            assertTrue(version1);
            assertTrue(version2);
        }finally{
            deleteDocument(doc);
        }
    }
    
    @Test
    public void deleteDocumentByVersion() throws IOException{
        logger.debug("Init deleteDocumentByVersion test");
        createSessionIfNeeded();
        String fileName = getFileName();
        String folderName = testProperties.getUsername();        
        byte[] contentFile = getFile();        
        Document doc = null;
        Document doc2;
        try{
            doc = AlfrescoAPI.createDocument(session, getFolder(folderName), fileName, contentFile, PDF_MIME_TYPE);                            
            byte[] newContentFile = "Nuevo documento de texto".getBytes();
            doc = getDocument(doc.getId());
            doc2 = AlfrescoAPI.updateDocument(session, doc, newContentFile, PLAINTEXT_MIME_TYPE, null, true, "a major change");                       
            if (doc2 == null){
                doc2 = AlfrescoAPI.updateDocument(session, doc, newContentFile, PLAINTEXT_MIME_TYPE, null, true, "a major change");                       
            }
            assertNotNull(doc2);
            String version = doc2.getVersionLabel();
            AlfrescoAPI.deleteDocumentByVersion(session, doc.getId(), version);
            List<Document> versions = AlfrescoAPI.getAllVersionsOfDocument(session, doc.getId());
            assertNotNull(versions);
            assertTrue(versions.size() == 1);
            assertEquals(versions.get(0).getVersionLabel(), "1.0");
        }finally{
            deleteDocument(doc);
        }
    }
    
    
    @Test
    public void getDocumentByVersion() throws IOException{
        logger.debug("Init updateDocument test");
        createSessionIfNeeded();
        String fileName = getFileName();
        String folderName = testProperties.getUsername();        
        byte[] contentFile = getFile();        
        Document doc = null;
        try{
            doc = AlfrescoAPI.createDocument(session, getFolder(folderName), fileName, contentFile, PDF_MIME_TYPE);                            
            String version = doc.getVersionLabel();
            byte[] newContentFile = "Nuevo documento de texto".getBytes();
            doc = getDocument(doc.getId());
            Document updatedDocument = AlfrescoAPI.updateDocument(session, doc, newContentFile, PLAINTEXT_MIME_TYPE, null, true, "a major change");                       
            if (updatedDocument == null){
                updatedDocument = AlfrescoAPI.updateDocument(session, doc, newContentFile, PLAINTEXT_MIME_TYPE, null, true, "a major change");                       
            }
            assertNotNull(updatedDocument);
            Document docVersion = AlfrescoAPI.getDocumentByVersion(session, doc.getId(), version);
            assertNotNull(docVersion);
            assertEquals(docVersion.getVersionLabel(), "1.0");
        }finally{
            deleteDocument(doc);
        }
    }
        
}