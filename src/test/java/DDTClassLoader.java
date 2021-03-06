import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Created with IntelliJ IDEA.
 * User: Avraham (Bey) Melamed
 * Date: 07/14/14
 * Time: 9:22 PM
 * Selenium Based Automation
 *
 * =============================================================================
 * Copyright 2014 Avraham (Bey) Melamed.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =============================================================================
 *
 * Description - Provides class loading from a project / folder other than the main project (this one.)
 * This provides the ability to develop inline test string provider classes apart from the main project and load these from their own project's test-classes folder (or another location.)
 *
 * History
 * When         |Who      |What
 * =============|=========|====================================
 * 07/14/14     |Bey      |Initial Version
 * =============|=========|====================================
 */

public class DDTClassLoader extends ClassLoader {

   private static String defaultLoadFolder;
   private static String loadableClassNames;

   private String errors;

   public static void setDefaultLoadFolder(String value) {
      defaultLoadFolder = value;
   }

   public static String getDefaultLoadFolder() {
      if (isBlank(defaultLoadFolder))
         setDefaultLoadFolder("c:/javaddtext/target/test-classes/");
      return defaultLoadFolder;
   }

   public DDTClassLoader(ClassLoader parent) {
      super(parent);
   }

   private void setErrors(String value) {
      errors = value;
   }

   public String getErrors() {
      if (isBlank(errors))
         setErrors("");
      return errors;
   }

   public static void setLoadableClassNames(String value) {
      loadableClassNames = value;
   }

   public static String getLoadableClassNames() {
      if (isBlank(loadableClassNames))
         setLoadableClassNames(findLoadableClassNames());
      return loadableClassNames;
   }

   /**
    * Get the loadable classes from the default class load folder
    * The folder is a folder where java .class files exist
    *
    * @return a String of class files delimited by "," and enclosed in "," (or an empty string) - if any, the base classes and other invalid ones for the purpose are ignored.
    */
   public static String findLoadableClassNames() {
      String loadFromFolder = getDefaultLoadFolder();
      String result = "";
      String excludedClassNames = ",TestStringsProvider.class,InlineTestStringsProvider.class,DDTClassLoader.class,TestStringsProviderSpecs.class,".toLowerCase();
      File loadFromFolderFile = new File(loadFromFolder);
      if (loadFromFolderFile.exists()) {
         File[] fileList = loadFromFolderFile.listFiles();
         int size = fileList.length;
         if (size > 0) {
            for (int i = 0; i < size; i++) {
               File theFile = fileList[i];
               String theName = theFile.getName();
               if (theName.toLowerCase().endsWith(".class")) {
                  if (!excludedClassNames.contains(theName.toLowerCase())) {
                     result += theName + ",";
                  }
               }
            }
            if (!isBlank(result)) {
               result = "," + result;
            }
         }
      }
      return result;
   }

   @Override
   /**
    * This is the overriding method used to load any desired external class
    * The logic of using this class loader or the parent has to do with the recursive nature of implementing class loader.
    * This particular class loader should be applied only to external classes whose parent classes exist in this project (not in the project of the external class)
    * Thus, parent classe(s) are loaded by the parent loader that CAN be recursive in this project
    * @return Class
    */
   public Class loadClass(String name)throws ClassNotFoundException {
      // Check the class name against a list of external classes
      if (getLoadableClassNames().contains(name)) {
         // Use external class loader (see below)
         return getClass(name);
      }
      // Use the parent's class loader.
      return super.loadClass(name);
   }

   /**
    *
    * @param name  - Full path specs of class name to load
    * @return      - A byte buffer representing the .class representation of the name.
    * @throws IOException
    */
   private byte[] loadClassFileData(String name) throws IOException {

      // Ensure proper separator and name extension for the class name to load
      String tmpName = name;
      if (tmpName.toLowerCase().endsWith(".class"))
         tmpName = tmpName.substring(0,tmpName.length()-6);
      tmpName = tmpName.replace('.', '/') + ".class";

      // Convert the file name to a URL
      URL myUrl;
      try {
         if (name.toLowerCase().startsWith("http:")) {
            tmpName = DDTSettings.asValidOSPath(tmpName, false);
            myUrl = new URL(tmpName);
         }
         else
            myUrl = new URL("file:" + tmpName);

         URLConnection connection = myUrl.openConnection();
         InputStream input = connection.getInputStream();
         ByteArrayOutputStream buffer = new ByteArrayOutputStream(8192);
         int data = input.read();

         while (data != -1) {
            buffer.write(data);
            data = input.read();
         }

         input.close();

         byte[] classData = buffer.toByteArray();
         return classData;
      }
      catch (IOException e) {
         setErrors(e.getMessage().toString());
      }
      catch (Exception e) {
         setErrors(e.getMessage().toString());
      }
      return null;
   }

   /**
    * Loads the class from the file system. The class file should be located in
    * the file system. The name should be relative to get the file location
    *
    * @param name
    *            Fully Classified name of class, for example com.dynabytes.TestStringsGenerator
    */
   private Class getClass(String name) throws ClassNotFoundException {
      String file =  getDefaultLoadFolder() + name.replace('.', File.separatorChar) + ".class";
      byte[] b = null;
      try {
         // This loads the byte code data from the physical file
         b = loadClassFileData(file);
         // defineClass is inherited from the ClassLoader class - it converts byte array into a Class.
         // defineClass is Final so we cannot override it
         Class c = defineClass(name, b, 0, b.length);
         resolveClass(c);
         return c;
      }
      catch (IOException e) {
         setErrors(e.getMessage().toString());
         return null;
      }
   }
}
