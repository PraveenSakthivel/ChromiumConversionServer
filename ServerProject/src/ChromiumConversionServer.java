import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.json.simple.*;
import org.json.simple.parser.*;

public class ChromiumConversionServer {

  private String targetPath = "Insert directory for completed builds";
  private String versionNumber = new String();
  private String buildName = new String();
  private final String nwjsDirectoryWin = "Insert directory for nwjs windows template";
  private final String nwjsDirectoryMac = "Insert directory for nwjs windows template";
  private final String inputDirectory = "Insert source directory here";
  private final String sourcePath = "NAME OF APP + /Contents/Resources/app.nw";
  private final String jenkinsPath =
      "Link to jenkins environment";
  private final String zipDirectory = "Insert directory for zipped completed builds";
  public static final String storageConnectionString =
      "Insert Authentication String for an Azure Web Storage Account";
  private PrintWriter out;
  private boolean successful = true;
  private boolean psFinished = false;
  private boolean psFolder = false;
  private String serverName = "Insert Server Name Here";
  private int multiplicity;

  public static void main(String[] args) throws IOException {
    // TODO Auto-generated method stub
    ChromiumConversionServer convert = new ChromiumConversionServer();
    ServerSocket serverSocket = new ServerSocket(28);
    Socket clientSocket = serverSocket.accept();
    convert.resetVars();
    convert.out = new PrintWriter(clientSocket.getOutputStream(), true);
    convert.out.println(serverName + ": Connection established");
    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    String input = in.readLine();
    convert.buildName = input;
    convert.convert();
    clientSocket.close();
    serverSocket.close();
  }
  /** method starts conversion process */
  private void convert() {
    removeUnzipped();
    clearBuilds();
    download(jenkinsPath + buildName, zipDirectory + "/" + buildName);
    unzip(zipDirectory + "/" + buildName, zipDirectory);
    versionNumber = getVersion();
    macConversion();
    windowsConversion();
    if (successful) out.println(serverName + ": Build conversion completed");
    else out.println(serverName + ": Build conversion failed");
    zipDirectory(targetPath, targetPath + ".zip");
    uploadBuilds();
    out.println("200");
    out.flush();
    out.close();
  }
  /** Creates a new copy of the NW.js template for Mac Copies source build to that template */
  private void macConversion() {
    out.println(serverName + ": Starting Mac Conversion");
    File newDir = multipleBuilds(0);
    // Create new folder for this release
    newDir.mkdirs();
    targetPath = newDir.getPath();
    File nwjs = new File(nwjsDirectoryMac);
    try {
      FileUtils.copyDirectory(nwjs, newDir);
      File source = new File(inputDirectory);
      File destination = new File(targetPath + sourcePath);
      FileUtils.copyDirectory(source, destination);
      out.println(serverName + ": Successfuly completed Mac Conversion");
    } catch (IOException E) {
      out.println(serverName + ": Error finishing Mac Conversion");
      out.println(E);
      successful = false;
    }
  }

  /** Copies builds like macConversion() Initiates a powershell script to run DesktopAppConverter */
  private void windowsConversion() {
    out.println(serverName + ": Starting Windows Conversion");
    File newDir = new File(targetPath);
    File nwjs = new File(nwjsDirectoryWin);
    try {
      //Copy over the NWJS Template
      FileUtils.copyDirectory(nwjs, newDir);
      //Copy over the new build
      File source = new File(inputDirectory);
      File destination = new File(targetPath + "/nwjs");
      FileUtils.copyDirectory(source, destination);
      setVars();
      //Run Powershell scripts
      String command = "powershell -File \"C:/Users/pearson2/Documents/Convert.ps1\"";
      psFinished = false;
      Process powerShellProcess = Runtime.getRuntime().exec(command);
      powerShellProcess.getOutputStream().close();
      out.println(serverName + ": Start POWERSHELL");
      while (!psFinished) {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          out.println(serverName + ": Error:");
          out.println(e);
        }
        checkPSFinished();
      }
      out.println(serverName + ": End POWERSHELL");
      out.println(serverName + ": Successfuly completed Windows Conversion");
    } catch (IOException E) {
      out.println(serverName + ": Error finishing Windows Conversion");
      out.println(E);
      successful = false;
    }
  }

  /**
   * Parses the manifest file in the source Build
   *
   * @return a string representation of the version
   */
  private String getVersion() {
    String version = ".0";
    try {
      JSONParser parser = new JSONParser();
      Object obj = new Object();
      obj = parser.parse(new FileReader(inputDirectory + "/manifest.json"));
      JSONObject object = (JSONObject) obj;
      version = (String) object.get("version") + version;
    } catch (Exception E) {
      out.println(serverName + ":" + E);
    }
    return version;
  }

  /**
   * writes script variables onto a text file this variables are parsed and used by the powershell
   * script
   */
  private void setVars() {
    try {
      out.println(serverName + ": Setting Variables");
      File vars = new File("C:/Users/pearson2/Documents/vars.txt");
      if (vars.exists()) vars.delete();
      vars = new File("C:/Users/pearson2/Documents/vars.txt");
      vars.createNewFile();
      BufferedWriter writer =
          Files.newBufferedWriter(
              Paths.get(vars.getAbsolutePath()), StandardCharsets.UTF_8, StandardOpenOption.WRITE);
      writer.write("BuildPath=" + targetPath + "\n");
      writer.write("Version=" + versionNumber);
      writer.flush();
      writer.close();
      out.println(serverName + ": Finished setting variables");
    } catch (IOException E) {
      out.println(serverName + ": Error setting variables");
      out.println(E);
    }
  }

  /**
   * Downloads the requested build from the Jenkins
   *
   * @param urlStr Url to file location
   * @param file destination for file download
   */
  private void download(String urlStr, String file) {
    out.println(serverName + ": Downloading build files");
    try {
      URL url = new URL(urlStr);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      String encoded =
          Base64.getEncoder()
              .encodeToString(
                  "Insert Username/password".getBytes(StandardCharsets.UTF_8));
      conn.setRequestProperty("Authorization", "Basic " + encoded);
      ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
      File output = new File(file);
      output.createNewFile();
      FileOutputStream fos = new FileOutputStream(file);
      fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
      fos.close();
      rbc.close();
      out.println(serverName + ": Finished downloading build files");
    } catch (IOException E) {
      out.println(serverName + ": Error downloading build files");
      out.println(E);
    }
  }

  public void unzip(String zipFilePath, String destDirectory) {
    try {
      out.println(serverName + ": Unzipping build");
      File destDir = new File(destDirectory);
      if (!destDir.exists()) {
        destDir.mkdir();
      }
      ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
      ZipEntry entry = zipIn.getNextEntry();
      while (entry != null) {
        String filePath = destDirectory + File.separator + entry.getName();
        if (!entry.isDirectory()) {
          extractFile(zipIn, filePath);
        } else {
          File dir = new File(filePath);
          dir.mkdir();
        }
        zipIn.closeEntry();
        entry = zipIn.getNextEntry();
      }
      zipIn.close();
      out.println(serverName + ": Finished unzipping build");
    } catch (IOException E) {
      out.println(serverName + ": Error unzipping build");
      out.println(E);
    }
  }
  /**
   * Extracts a zip entry (file entry)
   *
   * @param zipIn
   * @param filePath
   * @throws IOException
   */
  private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
    byte[] bytesIn = new byte[1024];
    int read = 0;
    while ((read = zipIn.read(bytesIn)) != -1) {
      bos.write(bytesIn, 0, read);
    }
    bos.close();
  }
  /**
   * Zips any directory
   *
   * @param dir
   * @param zipDirName
   */
	private void zipDirectory(String sourcePath, String destinationPath) {
		try {
		out.println(serverName + ": Zipping build");
		File source = new File(sourcePath);
		File destination = new File(destinationPath);
		OutputStream archiveStream = new FileOutputStream(destination);
		ArchiveOutputStream archive = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream);
		Collection<File> fileList = FileUtils.listFiles(source, null, true);
		for (File file : fileList) {
			String entryName = getEntryName(source, file);
			ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
			archive.putArchiveEntry(entry);
			BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
			IOUtils.copy(input, archive);
			input.close();
			archive.closeArchiveEntry();
		}
		archive.finish();
		archiveStream.close();
		out.println(serverName + ": Finished zipping builds");
		}
		catch(IOException e) {
		out.println(serverName + ": Error zipping builds, IOException:");
		out.println(e);
		}
		catch(ArchiveException e) {
			out.println(serverName + ": Error zipping builds, ArchiveException:");
			out.println(e);			
		}
	}

	private String getEntryName(File source, File file) throws IOException {
		int index = source.getAbsolutePath().length() + 1;
		String path = file.getCanonicalPath();
		return path.substring(index);
	}
  /**
   * deletes the previous source builds Maintains the output directories so there are only 10
   * converted builds on the machine
   */
  private void clearBuilds() {
    out.println(serverName + ": Deleting old builds");
    File dir = new File(zipDirectory);
    for (File temp : dir.listFiles()) {
      deleteDirs(temp);
    }
    dir = new File(targetPath);
    if (dir.listFiles().length > 9) {
      File[] files = dir.listFiles();
      File oldestFile = files[0];
      for (int i = 1; i < files.length; i++) {
        if (oldestFile.lastModified() < files[i].lastModified()) oldestFile = files[i];
      }
      deleteDirs(oldestFile);
    }
    out.println(serverName + ": Finished deleting old builds");
  }

  /**
   * deletes all files including directories
   *
   * @param file
   */
  private void deleteDirs(File file) {
    if (file.isDirectory()) {
      for (File temp : file.listFiles()) {
        deleteDirs(temp);
      }
    }
    file.delete();
  }
  /**
   * returns a file name minus .zip
   *
   * @param input
   * @return
   */
  private String removeExtension(String input) {
    return input.substring(0, input.length() - 4);
  }

  /** removes all files that aren't compressed from the build directory */
  private void removeUnzipped() {
    out.println(serverName + ": Cleaning folders");
    File dir = new File(targetPath);
    for (File temp : dir.listFiles()) {
      String name = temp.getName();
      if (!(name.substring(name.length() - 4).equals(".zip"))) {
        deleteDirs(temp);
      }
    }
  }
  /** Uploads the builds to the Azure Storage Account: RealizeReaderBuilds */
  private void uploadBuilds() {
    try {
      out.println(serverName + ": Uploading converted builds");
      CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString);
      CloudBlobClient serviceClient = account.createCloudBlobClient();
      CloudBlobContainer container = serviceClient.getContainerReference("builds");
      container.createIfNotExists();
      CloudBlockBlob blob =
          container.getBlockBlobReference(
              removeExtension(buildName) + "(" + multiplicity + ")" + ".zip");
      File sourceFile = new File(targetPath + ".zip");
      blob.upload(new FileInputStream(sourceFile), sourceFile.length());
      out.println(serverName + ": Finished Uploading converted builds");
    } catch (Exception e) {
      out.println(serverName + ": Error uploading builds");
      out.println(e);
    }
  }
  /**
   * Checks to see if there are duplicates that have been built
   *
   * @param version
   * @return a new unused Directory
   */
  private File multipleBuilds(int version) {
    if (version == 0) {
      File dir = new File(targetPath + "/" + removeExtension(buildName));
      File zipDir = new File(dir.getAbsolutePath() + ".zip");
      if (zipDir.exists()) return multipleBuilds(1);
      else {
        multiplicity = version;
        return dir;
      }
    } else {
      File dir = new File(targetPath + "/" + removeExtension(buildName) + "(" + version + ")");
      File zipDir = new File(dir.getAbsolutePath() + ".zip");
      if (zipDir.exists()) {
        version++;
        return multipleBuilds(version);
      } else {
        multiplicity = version;
        return dir;
      }
    }
  }
  /**
   * reset the non final variables after a loop
   *
   * @throws IOException
   */
  private void resetVars() throws IOException {
    File output = new File("C:/users/pearson2/documents/powershellOutput.txt");
    output.delete();
    output.createNewFile();
  }

  private void checkPSFinished() {
    if (psFolder) {
      File cert = new File(targetPath + "Insert path to auto generated build certificate");
      if (cert.exists()) psFinished = true;
    } else {
      File folder = new File(targetPath + "/Insert Application Name");
      if (folder.exists()) psFolder = true;
    }
  }
}
