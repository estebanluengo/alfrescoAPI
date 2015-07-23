# alfrescoAPI
This project builds a Jar that contains one class with a collection of methods to work easily with the Alfresco Server or another Cmis server.
I have only tested this functionality with Alfresco Server 4.2.f. For Alfresco 5.0 it's necessary to change the createSession method and run the test. 
Maybe not all methods will work properly in Alfresco 5.0. 

Before execute the test, complete these steps:

1) Edit config.properties according to your Alfresco installation
2) Copy the files from the dataModel directory to the C:\Alfresco\tomcat\shared\classes\alfresco\extension directory 
The author of this dataModel is Jeff Potts and you can find an excellent tutorial at this link: http://ecmarchitect.com/alfresco-developer-series-tutorials/content/tutorial/tutorial.html