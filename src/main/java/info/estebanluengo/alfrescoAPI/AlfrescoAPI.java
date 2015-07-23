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
package info.estebanluengo.alfrescoAPI;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.chemistry.opencmis.client.api.CmisObject;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisStorageException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class that encapsulates access to the Alfresco server and offers a set of high level operations to
 * work easily with Alfresco or another Cmis server.
 * <br>
 * I've received inspiration to build this class after reading the Jeff Potts tutorial 
 * <a href="http://ecmarchitect.com/alfresco-developer-series-tutorials/content/tutorial/tutorial.html">Working With Custom Content Types in Alfresco</a>
 * 
 * @author Esteban Luengo Simón 
 * @version 1.0 11/May/2015
 */
public class AlfrescoAPI {

    //https://logging.apache.org/log4j/2.0/manual/api.html
    private static final Logger logger = LogManager.getLogger();
    
    public static String CUSTOM_ASSOCIATION = "R:cmiscustom:assoc"; //is a creatable sub-type of cmis:relationship
    public static String CUSTOM_DOCUMENT_TYPE = "D:cmiscustom:document"; //a subtype of cmis:document
    
    /**
     * Creates a new Session to allow access to the server. This method uses ATOMPUB binding type.
     * 
     * @param user a String that contains the username
     * @param password a String that contains the password
     * @param url an URL to the server. The URL has to be atompub style: 
     * <br>Example: <a href="http://host:port/alfresco/api/-default-/public/cmis/versions/1.1/atom">AtomPub url style</a>
     *
     * @return a Session object that allow access to the Alfresco Server or null if no session has been created
     */
    public static Session createSession(String user, String password, String url) {
        // default factory implementation
        logger.debug("createSession called");
        SessionFactory factory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<>();

        // user credentials
        parameter.put(SessionParameter.USER, user);
        parameter.put(SessionParameter.PASSWORD, password);

        // Specify the connection settings
        parameter.put(SessionParameter.ATOMPUB_URL, url);
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

        //Another way. This way works with http://host:port/alfresco/cmis/ URL style
//        parameter.put(SessionParameter.BINDING_TYPE, BindingType.WEBSERVICES.value());
//        String BASE_URL = url;
//        parameter.put(SessionParameter.WEBSERVICES_ACL_SERVICE, BASE_URL + "ACLService?wsdl");
//        parameter.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE, BASE_URL + "DiscoveryService?wsdl");
//        parameter.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE, BASE_URL + "MultiFilingService?wsdl");
//        parameter.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE, BASE_URL + "NavigationService?wsdl");
//        parameter.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, BASE_URL + "ObjectService?wsdl");
//        parameter.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, BASE_URL + "PolicyService?wsdl");
//        parameter.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE, BASE_URL + "RelationshipService?wsdl");
//        parameter.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE, BASE_URL + "RepositoryService?wsdl");
//        parameter.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE, BASE_URL + "VersioningService?wsdl");

//        // set the alfresco object factory. Not for Alfresco 4.2, only for Alfresco 5.x
//        parameter.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");        
        List<Repository> repositories = factory.getRepositories(parameter);
        logger.debug("getting repositories");
        // create session
        Session session = repositories.get(0).createSession();
        logger.debug("returning a session object");
        return session;
    }

    /**
     * Retrieves the folder with the given name and exists under the parentFolder. 
     * Returns null if the folder does not exist.
     * 
     * @param session a Session object that is connected with the server
     * @param parentFolder a Folder object that represents parent folder where the folder to
     * retrieve exists
     * @param folderName a String that contains the folder name
     * 
     * @return a Folder object that represent the folder in the Alfresco repository
     */
    public static Folder getFolderByName(Session session, Folder parentFolder, String folderName) {
        logger.debug("getFolder called for folderName:"+folderName);
        ObjectType type = session.getTypeDefinition(BaseTypeId.CMIS_FOLDER.value());
        PropertyDefinition<?> objectIdPropDef = type.getPropertyDefinitions().get(PropertyIds.OBJECT_ID);
        String objectIdQueryName = objectIdPropDef.getQueryName();
        String query = "SELECT * FROM cmis:folder WHERE cmis:name='" + folderName + "' and IN_FOLDER('workspace://SpacesStore/"+parentFolder.getId()+"')";
        ItemIterable<QueryResult> results = session.query(query, false);
        logger.debug("query executed:"+query);
        for (QueryResult qResult : results) {
            String objectId = qResult.getPropertyValueByQueryName(objectIdQueryName);
            logger.debug("ObjectId recovered from query:"+objectId);
            return (Folder) session.getObject(session.createObjectId(objectId));
        }
        logger.debug("No results recover for query");
        return null;
    }
    
    /**
     * Retrieves the folder with the given name. Returns null if the folder does
     * not exist. If there are more than one folder with the same name, the method
     * returns the first folder in the list
     * 
     * @param session a Session object that is connected with the server
     * @param folderName a String that contains the folder name
     * 
     * @return a Folder object that represent the folder in the Alfresco repository
     */
    public static Folder getFolderByName(Session session, String folderName) {
        logger.debug("getFolder called for folderName:"+folderName);
        ObjectType type = session.getTypeDefinition(BaseTypeId.CMIS_FOLDER.value());
        PropertyDefinition<?> objectIdPropDef = type.getPropertyDefinitions().get(PropertyIds.OBJECT_ID);
        String objectIdQueryName = objectIdPropDef.getQueryName();
        String query = "SELECT * FROM cmis:folder WHERE cmis:name='" + folderName + "'";
        ItemIterable<QueryResult> results = session.query(query, false);
        logger.debug("query executed:"+query);
        for (QueryResult qResult : results) {
            String objectId = qResult.getPropertyValueByQueryName(objectIdQueryName);
            logger.debug("ObjectId recovered from query:"+objectId);
            return (Folder) session.getObject(session.createObjectId(objectId));
        }
        logger.debug("No results recover from query");
        return null;
    }

    /**
     * Creates a new folder under the parentFolder. 
     * 
     * @param session a Session object that is connected with the server
     * @param parentFolder a Folder object where the new folder will be created
     * @param folderName a String that contains the name for the new folder
     * 
     * @return a Folder object that represent the new folder created
     * @throws CmisContentAlreadyExistsException will be thrown if the folder to be created exists in the same parentFolder.
     */
    public static Folder createFolder(Session session, Folder parentFolder, String folderName) throws CmisContentAlreadyExistsException {
        logger.debug("createFolder called");
        Map<String, Object> folderProps = new HashMap<>();
        folderProps.put(PropertyIds.NAME, folderName);
        folderProps.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_FOLDER.value());

        ObjectId folderObjectId = session.createFolder(folderProps, parentFolder, null, null, null);
        logger.debug("Folder created with id:"+folderObjectId);
        return (Folder) session.getObject(folderObjectId);
    }
    
    /**
     * Creates the folders under the parentFolder. The method creates the folders that don't exist in the
     * tree. Maybe if all folders exist under the parentFolder, no folder is created.
     * 
     * @param session a Session object that is connected with the server
     * @param parentFolder a Folder object where the new folder will be created
     * @param foldersPath a String that contains the folders path to be created. For example: folder1/folder2
     * 
     * @return a Folder object that represent the last folder under the tree
     */
    public static Folder createFolders(Session session, Folder parentFolder, String foldersPath) {
        logger.debug("createFolders called");
        Folder folder = parentFolder;
        String[] folderList = foldersPath.split("/");
        for (String f : folderList) {
            logger.debug(folder.getPath() + "/" + f);            
            try{
                CmisObject objFolder = session.getObjectByPath(folder.getPath() + "/" + f);
                folder = (Folder)objFolder;
            }catch(CmisObjectNotFoundException e){
                folder = createFolder(session, folder, f);
            }            
        }
        return folder;
    }
    
    /**
     * Gets the folders that exits in the folder
     * 
     * @param session a Session object that is connected with the server
     * @param parentFolder a Folder object where we want the children folders
     * @param cache a boolean. True indicates that cache is used and false indicates that the
     * folders are retrieved from the server     
     * @return a List<org.apache.chemistry.opencmis.client.api.Folder> that contain the folder list
     */
    public static List<Folder> getFolders(Session session, Folder parentFolder, boolean cache){
        OperationContext oc = session.createOperationContext();
        oc.setCacheEnabled(cache);
        ItemIterable<CmisObject> children = parentFolder.getChildren(oc);
        List<Folder> folderList = new ArrayList<>();
        for (CmisObject obj: children){
            if (obj instanceof Folder){
                folderList.add((Folder)obj);
            }
        }
        return folderList;
    }

    /**
     * Creates a new Document in the folder with the name, content, and mimeType. 
     * 
     * @param session a Session object that is connected with the server
     * @param folder a Folder object where the new document will be created
     * @param fileName a String that contain the file name
     * @param content a byte array that contain the document
     * @param mimeType a String that represent the mime type of the document
     * 
     * @return a Document object that represent the document that has just been created
     * @throws CmisContentAlreadyExistsException if the document to be created exists in the same folder
     */
    public static Document createDocument(Session session, Folder folder, String fileName, byte[] content, String mimeType) 
                                            throws CmisContentAlreadyExistsException{
        logger.debug("createDocument called for document name:"+fileName);
        Map<String, Object> docProps = new HashMap<>();
        docProps.put(PropertyIds.NAME, fileName);
        docProps.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
        docProps.put(PropertyIds.CREATION_DATE, new Date());

        ByteArrayInputStream in = new ByteArrayInputStream(content);
        ContentStream contentStream = new ContentStreamImpl(fileName, BigInteger.valueOf(content.length), mimeType, in);

        ObjectId documentId = session.createDocument(docProps, session.createObjectId((String) folder.getPropertyValue(PropertyIds.OBJECT_ID)), contentStream, null, null, null, null);
        logger.debug("Document created with id:"+documentId.getId());
        Document document = (Document) session.getObject(documentId);
        return document;
    }
        
    /**
     * Creates a new Document in the folder with the name, content, and mimeType, docType and properties given in the method call
     * 
     * @param session a Session object that is connected with the server
     * @param folder a Folder object where the new document will be created
     * @param fileName a String that contain the file name
     * @param content a byte array that contain the document
     * @param mimeType a String that represent the mime type of the document
     * @param docType a String that represent the document type. If it is null then CUSTOM_DOCUMENT_TYPE will be used
     * @param docProps a Map with the properties to are associated to the document. It may be null
     * 
     * @return a Document object that represent the document that has just been created
     * @throws CmisContentAlreadyExistsException if the document to be created exists in the same folder
     */
    public static Document createDocument(Session session, Folder folder, String fileName, byte[] content, String mimeType,
            String docType, Map<String, Object> docProps) throws CmisContentAlreadyExistsException{
        logger.debug("createDocument called for document name:"+fileName);
        if (docProps == null){
            docProps = new HashMap<>();
        }
        docProps.put(PropertyIds.NAME, fileName);
        docProps.put(PropertyIds.OBJECT_TYPE_ID, docType == null?CUSTOM_DOCUMENT_TYPE:docType);        
        ByteArrayInputStream in = new ByteArrayInputStream(content);
        ContentStream contentStream = new ContentStreamImpl(fileName, BigInteger.valueOf(content.length), mimeType, in);
        ObjectId documentId = session.createDocument(docProps, session.createObjectId((String) folder.getPropertyValue(PropertyIds.OBJECT_ID)), contentStream, VersioningState.MAJOR);
        logger.debug("Document created with id:"+documentId.getId());
        Document document = (Document) session.getObject(documentId);
        return document;
    }
    
    /**
     * Relates the source document with the target document. After calling to this method you can access to
     * the target document from the source document but not viceversa
     * 
     * @param session a Session object that is connected with the server
     * @param sourceDoc a Document that represent the source document. It can not be null
     * @param targetDoc a Document that represent the target document. It can not be null
     * @param associationName a String that represent the association name. If it is null then the method uses R:cmiscustom:assoc
     */
    public static void relatedDocuments(Session session, Document sourceDoc, Document targetDoc, String associationName) {
        logger.debug("relatedDocument called");
        String sourceId = sourceDoc.getId();
        String targetId = targetDoc.getId();
        Map<String, String> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, associationName == null?CUSTOM_ASSOCIATION:associationName);
        properties.put(PropertyIds.SOURCE_ID, sourceId);
        properties.put(PropertyIds.TARGET_ID, targetId);
        session.createRelationship(properties);
        logger.debug("relationShip created between the two documents");
    }
    
    /**
     * Updates the document that exits in the server and creates a new version. The method allows to update the content of the document, 
     * the mimetype and the properties.<br>
     * In some circunstances a CmisStorageException could be thrown with the message "Expected x bytes but retrieved 0 bytes!". I Think
     * this is an issue in Alfresco Server and it can be resolved catching the exception and calling this method another time.<br> 
     * <a href="https://issues.alfresco.com/jira/browse/ACE-2821">Link to the issue</a>
     * 
     * @param session a Session object that is connected with the server
     * @param doc a Document object to be updated.
     * @param newContent a byte[] with the content of the document. It can not be null.
     * @param mimeType a String that represent the mime type of the document
     * @param docProps a Map object with the properties of the document. It can be null
     * @param majorVersion a boolean. true indicates that we want a major version and false a minor version.
     * @param checkinComment a String with the comments that are associated to the new version
     * 
     * @return a Document that contains the new version created or null if it was not possible to make a new version.
     * Remember that every version of the document may has its own ID depending of the server. Alfresco uses alfhanumeric plus ;version
     */
    public static Document updateDocument(Session session, Document doc, byte[] newContent, String mimeType, 
            Map<String, Object> docProps, boolean majorVersion, String checkinComment){
        logger.debug("updateDocument called for docId:"+doc.getId()+" length:"+newContent.length);
        Document updatedDocument = null;
        if (doc.getAllowableActions().getAllowableActions().contains(org.apache.chemistry.opencmis.commons.enums.Action.CAN_CHECK_OUT)) {            
            doc.refresh();    
            //make a checkout is mandatory for some repositories. 
            ObjectId checkedOutDocument = doc.checkOut();
            Document pwc = (Document) session.getObject(checkedOutDocument);
            ByteArrayInputStream in = new ByteArrayInputStream(newContent);            
            ObjectId objectId;
            try{
                ContentStream contentStream = new ContentStreamImpl(doc.getName(), BigInteger.valueOf(newContent.length), mimeType, in);
                objectId = pwc.checkIn(majorVersion, docProps, contentStream, checkinComment);
            }catch(CmisStorageException e){
                logger.error("Error trying to make a checkIn", e);
                pwc.delete();
                return null;
            }
            updatedDocument = (Document) session.getObject(objectId);            
        }
        return updatedDocument;
    }
    
    /**
     * Updates the document properties of the document that exist in the server
     * 
     * @param session a Session object that is connected with the server     
     * @param doc a Document object to be updated.          
     * @param updateProperties a Map object with the new properties of the document. It can be null
     * @return a Document object with the new properties. This object is retrieved from the server and not from the cache
     */
    public static Document updateDocumentProperties(Session session, Document doc, Map<String, Object> updateProperties){
        logger.debug("updateDocumentProperties called");
        ObjectId docReturned = doc.updateProperties(updateProperties, true);
        logger.debug("Document updated with id:"+doc.getId());
        Document document = (Document) session.getObject(docReturned);
        return document;
    }

    /**
     * Deletes the document from the server
     * 
     * @param session a Session object that is connected with the server
     * @param docId a String that represent the document Id in the server
     * @param allVersions a boolean. True value indicates that all versions of the document will be deleted. False
     * indicates that only the version of the document will be deleted
     */
    public static void deleteDocument(Session session, String docId, boolean allVersions){
        logger.debug("deleteDocument called");
        String repositoryId = session.getRepositoryInfo().getId();
        session.getBinding().getObjectService().deleteObject(repositoryId, docId, allVersions, null);        
    }
        
    /**
     * Deletes the version of the document from the server
     * 
     * @param session a Session object that is connected with the server
     * @param docId a String that represent the document Id in the server
     * @param version a String that represent the version of the document to be deleted
     */
    public static void deleteDocumentByVersion(Session session, String docId, String version){       
        logger.debug("deleteDocumentByVersion called for docId:"+docId);
        List<Document> versions = getAllVersionsOfDocument(session, docId);
        for (Document doc: versions){
            if (version.equals(doc.getVersionLabel())){
                doc.delete(false);
            }
        }
    }
    
    /**
     * Deletes the folder from the server and all the documents that are under the folder.       
     * 
     * @param session a Session object that is connected with the server
     * @param folderId a String that represents the forlder Id to be deleted
     * @throws CmisConstraintException If the folder to be deleted contains other folders 
     */
    public static void deleteFolder(Session session, String folderId) throws CmisConstraintException{
        logger.debug("deleteFolder called");
        session.delete(session.createObjectId(folderId), true);
    }
    
    /**
     * Deletes the folder from the server and all the documents that are under the folder.  
     * 
     * @param session a Session object that is connected with the server
     * @param folder a Folder object to be deleted
     * @throws CmisConstraintException If the folder to be deleted contains other folders
     */
    public static void deleteFolder(Session session, Folder folder) throws CmisConstraintException{
        logger.debug("deleteFolder called");
        session.delete(folder, true);
    }
    
    /**
     * Deletes the folder from the server and all the documents and subfolders that are under the folder.  
     * 
     * @param session a Session object that is connected with the server
     * @param folder a Folder object to be deleted
     * @param allVersions a boolean. True value indicates that all versions of the folder will be deleted.
     * False indicates that only this version of the folder will be deleted
     */
    public static void deleteFolder(Session session, Folder folder, boolean allVersions){
        logger.debug("deleteFolder called");
        /**
         * Note that with the continueOnFailure parameter set to true, folders and documents are deleted individually. If a document or folder 
         * cannot be deleted, the method moves to the next document or folder in the list. When the method completes, it returns a list of the document 
         * IDs and folder IDs that were not deleted.
         * With the continueOnFailure parameter set to false, all of the folders and documents can be deleted in a single batch, which, depending on 
         * the repository design, may improve performance. If a document or folder cannot be deleted, an exception is raised. Some repository implementations 
         * will attempt the delete transactionally, so if it fails, no objects are deleted. In other repositories a failed delete may have deleted some, 
         * but not all, objects in the tree.
         */
        folder.deleteTree(allVersions, UnfileObject.DELETE, true);
    }
        
    /**
     * Deletes all subfolders and document from the folder
     * 
     * @param session a Session object that is connected with the server
     * @param folder a Folder object where we want to delete all children
     * @param allVersions a boolean. True value indicates that all versions of the folder will be deleted.
     * False indicates that only this version of the folder will be deleted
     */
    public static void deleteChildren(Session session, Folder folder, boolean allVersions){
        logger.debug("deleteChildren called");
        ItemIterable<CmisObject> children = folder.getChildren();
        for (CmisObject obj: children){
            if (obj instanceof Folder){
                AlfrescoAPI.deleteFolder(session, (Folder)obj, allVersions);
            }else if (obj instanceof Document){
                AlfrescoAPI.deleteDocument(session, ((Document)obj).getId(), allVersions);
            }
        }
    }
    
    /**
     * Gets the document from the server without using the cache system. 
     * 
     * @param session a Session object that is connected with the server
     * @param docId a String that represent the document Id
     * @return a Document object with the document
     * @throws CmisObjectNotFoundException will be thrown if the document does not exist in the server
     */
    public static Document getDocument(Session session, String docId) throws CmisObjectNotFoundException{
        return getDocument(session, docId, false);
    }
    
    /**
     * Gets the document from the server or from the cache. 
     * 
     * @param session a Session object that is connected with the server
     * @param docId a String that represent the document Id
     * @param cache a boolean. True indicates that cache is used and false indicates that the
     * document is retrieved from the server     
     * @return a Document object with the document
     * @throws CmisObjectNotFoundException will be thrown if the document does not exist in the server
     */
    public static Document getDocument(Session session, String docId, boolean cache) throws CmisObjectNotFoundException{
        logger.debug("getDocument called for id:"+docId);
        OperationContext oc = session.createOperationContext();
        oc.setCacheEnabled(cache);
        CmisObject object = session.getObject(docId, oc);
        logger.debug("document recovered");
        return (Document) object;
    }
    
    /**
     * Gets a specific version of the document or null if does not exist this version. This method
     * does not use the cache.
     * 
     * @param session a Session object that is connected with the server
     * @param docId a String that represent the document Id
     * @param version a String that represent the version of the document to be retrieved
     * @return a Document object with the document
     */
    public static Document getDocumentByVersion(Session session, String docId, String version) {
        return getDocumentByVersion(session, docId, version, false);
    }
    
    /**
     * Gets a specific version of the document or null if does not exist this version
     * 
     * @param session a Session object that is connected with the server
     * @param docId a String that represent the document Id
     * @param version a String that represent the version of the document to be retrieved
     * @param cache a boolean. True indicates that cache is used and false indicates that the
     * document is retrieved from the server     
     * @return a Document object with the document
     */
    public static Document getDocumentByVersion(Session session, String docId, String version, boolean cache) {
        logger.debug("getDocumentByVersion called for id:"+docId+" and version:"+version);
        List<Document> versions = getAllVersionsOfDocument(session, docId, cache);
        for (Document doc: versions){
            if (version.equals(doc.getVersionLabel())){
                logger.debug("Document recovered");
                return doc;
            }
        }
        return null;
    }
        
    /**
     * Gets the content of the document 
     * 
     * @param session a Session object that is connected with the server
     * @param docId a String that represent the document Id
     * @return a byte[] that represents the content of the document
     * @throws java.io.IOException if the content cannot be read from the document
     * @throws CmisObjectNotFoundException will be thrown if the document does not exist in the server 
     */
    public static byte[] getDocumentContent(Session session, String docId) throws CmisObjectNotFoundException, IOException {
        logger.debug("getDocumentContent called");
        Document doc = getDocument(session, docId);
        if (doc.getContentStreamLength() == 0){
            return null;
        }
        ContentStream contentStream = doc.getContentStream();
        try(InputStream inputStream = contentStream.getStream()){            
            byte[] content = IOUtils.toByteArray(inputStream);
            logger.debug("Content recovered");
            return content;
        }catch(IOException e){
            throw e;
        }        
    }
    
    /**
     * Gets the document and all its relationships from the server or from the cache. 
     * 
     * @param session a Session object that is connected with the server
     * @param docId a String that represent the document Id
     * @param cache a boolean. True indicates that cache is used and false indicates that the
     * document is retrieved from the server     
     * @return a Document object with the document
     * @throws CmisObjectNotFoundException if the document does not exist in the server
     */
    public static Document getDocumentWithRelationShips(Session session, String docId, boolean cache) throws CmisObjectNotFoundException{
        logger.debug("getDocumentWithRelationShips called for id:"+docId);
        OperationContext oc = session.createOperationContext();
        oc.setCacheEnabled(cache);
        oc.setIncludeRelationships(IncludeRelationships.SOURCE);
        Object object = session.getObject(session.createObjectId(docId), oc);
        logger.debug("document recovered");
        return (Document)object;
    }
    
    /**
     * Gets all versions of the document from the server without using the cache
     * 
     * @param session a Session object that is connected with the server
     * @param docId a String that represent the document Id
     * @return a List<org.apache.chemistry.opencmis.client.api.Document> with all versions of the document
     * @throws CmisObjectNotFoundException if the document does not exist in the server
     */
    public static List<Document> getAllVersionsOfDocument(Session session, String docId) throws CmisObjectNotFoundException {
        return getAllVersionsOfDocument(session, docId, false);
    }
    
    /**
     * Gets all versions of the document from the server or from the cache
     *
     * @param session a Session object that is connected with the server
     * @param docId a String that represent the document Id
     * @param cache a boolean. True indicates that cache is used and false indicates that the
     * document is retrieved from the server     
     * @return a List<org.apache.chemistry.opencmis.client.api.Document> with all versions of the document     
     * @throws CmisObjectNotFoundException if the document does not exist in the server
     */
    public static List<Document> getAllVersionsOfDocument(Session session, String docId, boolean cache) throws CmisObjectNotFoundException {
        logger.debug("getAllVersionsOfDocument called for docId:"+docId);    
        Document document = getDocument(session, docId, cache);                
        return document.getAllVersions();        
    }

    /**
     * Executes a query in the server or in the cache to retrive a list of CmisObjects
     * 
     * @param session a Session object that is connected with the server
     * @param query a String that contains the query to be executed
     * @param maxNumItems a int that represent the max number of items to be retrieved. If this value is equal to 0 then there
     * is no limitation.
     * @param cache a boolean. True indicates that cache is used and false indicates that the
     * query is executed in the server     
     * @return a List<org.apache.chemistry.opencmis.client.api.CmisObject> that contains the list
     * of CmisObject to be returned
     */
    public static List<CmisObject> executeQuery(Session session, String query, int maxNumItems, boolean cache) {
        logger.debug("getQueryResults called for query:"+query);    
        List<CmisObject> objList = new ArrayList<>();    	
        OperationContext context = session.createOperationContext();
        context.setCacheEnabled(cache);
    	// execute query
    	ItemIterable<QueryResult> results = session.query(query, false, context);
        logger.debug("items:"+results.getPageNumItems());
        if (maxNumItems == 0){
            results = results.getPage();
        }else{                
            results = results.getPage(maxNumItems);
        }
        logger.debug("query executed");
    	for (QueryResult qResult : results) {
            String objectId;
            PropertyData<?> propData = qResult.getPropertyById("cmis:objectId"); // Atom Pub binding
            if (propData != null) {
                    objectId = (String) propData.getFirstValue();
            } else {
                    objectId = qResult.getPropertyValueByQueryName("d.cmis:objectId"); // Web Services binding
            }
            logger.debug("Object recovered from query with id:"+objectId);
            CmisObject obj = session.getObject(session.createObjectId(objectId));
            objList.add(obj);
    	}
    	return objList;
    }
    
    /**
     * Finds a list of documents that contain the keyword. This method makes a full scan.
     * 
     * @param session a Session object that is connected with the server
     * @param keyword a String that represent the word to use in the full scan
     * @param maxNumItems a int that represent the max number of items to be retrieved. If this value is equal to 0 then there
     * is no limitation.
     * @param cache a boolean. True indicates that cache is used and false indicates that the
     * query is executed in the server     
     * @return a List<org.apache.chemistry.opencmis.client.api.CmisObject> that contains the list
     * of CmisObject to be returned      
     */
    public static List<CmisObject> findDocumentsByText(Session session, String keyword, int maxNumItems, boolean cache) {
        logger.debug("getQueryResults called for keyword:"+keyword);
        return executeQuery(session, "select * from cmis:document where contains('"+keyword+"')", maxNumItems, cache);
    }
    
    /**
     * Finds the documents that are in the folder
     * 
     * @param session a Session object that is connected with the server
     * @param folder a Folder object where we want to get the documents
     * @param maxNumItems a int that represent the max number of items to be retrieved. If this value is equal to 0 then there
     * is no limitation.
     * @param cacheEnable
     * @return a List<org.apache.chemistry.opencmis.client.api.CmisObject> that contains the list
     * of CmisObject to be returned      
     */
    public static List<CmisObject> findDocumentsInFolder(Session session, Folder folder, int maxNumItems, boolean cacheEnable) {
        logger.debug("getQueryResults called for folderName:"+folder.getName());
        return executeQuery(session, "select * from cmis:document where IN_FOLDER('workspace://SpacesStore/"+folder.getId()+"')", maxNumItems, cacheEnable);
    }
    
}