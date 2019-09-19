package common.util;
import java.io.*;
import java.nio.channels.*;

public class FileUtil {
	
	/**
	 * set file permission
	 * 
	 * @param strDest
	 *			target path
	 * @return boolean
	 */
	public static boolean setPermission(String strDest) {
		
		boolean	 bRet 		= true;
		String	strResult 	= "";
		Process proc		= null;
		
		try	{    
			Runtime rt = Runtime.getRuntime();
			if( rt != null ) {
				proc = rt.exec((new File(strDest).isFile())?"chmod 766 "+strDest:"chmod 777 "+strDest);
				if( proc != null ) {
					BufferedReader 	br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
					if( br != null ) {
						String line = null;
						while ( (line = br.readLine()) != null) {
							strResult += line;
						}    
						
						if( strResult == "" || strResult == null )	
							bRet = true;
						
						proc.getErrorStream().close();
						proc.getInputStream().close();
						proc.getOutputStream().close();
						proc.waitFor();
					}
				} else {
					bRet = false;
				}
			}
		} catch(Throwable t) {
			t.printStackTrace();
			bRet = false;
		} 
		return bRet;
	}
	
	/**
	 * check existence file or folder
	 * 
	 * @param strPath
	 *			target path
	 * @return boolean
	 */
	public static boolean isExist( String strPath )	{
		boolean bRet = true;
		try {
			if(new File(strPath).exists())	bRet = true;
			else							bRet = false;
		} catch(Exception e) {
			bRet = false;
		}
		return bRet;
	}
	
	/**
	 * make directory
	 * 
	 * @param strDir
	 *			target path
	 * @return boolean
	 */
	public static boolean makeDir( String strDir ) {
		try {
			File dirDest = new File(strDir);
			if(dirDest.isFile())
				return false;
			
			if(!dirDest.exists()) {
					return dirDest.mkdirs();
			} else	return true;
		} catch(Exception e) {
			return false;
		}
	}
	
	/**
	 * delete file
	 * 
	 * @param strPath
	 *			target path
	 * @return boolean
	 */
	public static boolean deleteFile( String strPath ) {
		try {
			File dirDest = new File(strPath);
			if(dirDest.isFile() && dirDest.exists() && dirDest.canWrite()) {
				System.gc();
				if(dirDest.delete()) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}

//			if(dirDest.exists() && dirDest.canWrite() && dirDest.isFile()) {
//				System.gc();
//				return dirDest.delete();
//			} else {
//				return false;
//			}
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * delete file
	 * 
	 * @param file
	 *			target File
	 * @return boolean
	 */
	public static boolean deleteFile( File file ) {
		try {
			File dirDest = file;
			if(dirDest.exists() && dirDest.canWrite()) {
				System.gc();
				return dirDest.delete();
			} else {
				return false;
			}
		} catch(Exception e) {
			return false;
		}
	}
	
	/**
	 * delete folder
	 * 
	 * @param strDir
	 *			target path
	 * @return boolean
	 */
	public static boolean deleteFolder(String strDir) {
		try	{
			File targetFolder = new File(strDir);
			File[] childFile = targetFolder.listFiles();
		    int size = childFile.length;
		    if (size > 0) {
		        for (int i = 0; i < size; i++) 
		        {
		            if (childFile[i].isFile()) 
		            {
		                childFile[i].delete();
		            } else {
		            	deleteFolder(childFile[i].getAbsolutePath());
		            }
		        }
		    }
		    targetFolder.delete();	
		}	catch(Exception e)	{
			return false;
		}
		return true;
	}
	
	/**
	 * copy file
	 * 
	 * @param sourceFile
	 *			source file path
	 * @param targetFile
	 *			target file path
	 * @return boolean
	 */
	@SuppressWarnings("resource")
	public static boolean FileCopy(String sourceFile, String targetFile) throws IOException {
	  	boolean bRet			= false;
	  	FileChannel inChannel 	= null;
	    FileChannel outChannel 	= null;
	    try {
	    	inChannel 	= new FileInputStream(sourceFile).getChannel();
		    outChannel 	= new FileOutputStream(targetFile).getChannel();
			int 	maxCount 	= (64 * 1024 * 1024) - (32 * 1024);
	      	long 	size 			= inChannel.size();
	      	long 	position 	= 0;
	      	while (position < size) {
	        	position += inChannel.transferTo(position, maxCount, outChannel);
	  		}
	  		if( position >= size )	bRet = true;
	    } catch (IOException e) {
			throw e;
	    } finally {
			if( inChannel	!= null)	inChannel.close();	
			if( outChannel 	!= null)	outChannel.close();
	    }
	    return bRet;
	}
	
	/**
	 * copy folder
	 * 
	 * @param sourceFile
	 *			source directory path
	 * @param targetFile
	 *			target directory path
	 * @return boolean
	 */
	@SuppressWarnings("resource")
	public static boolean folderCopy(String sourcePath, String targetPath) throws IOException {

		boolean bRet 			= false;
		FileChannel inChannel 	= null;
	    FileChannel outChannel 	= null;
		
	    File sourceF = new File(sourcePath);
		File targetF = new File(targetPath);
		
		File[] sourceFileList = null;
		
		if(sourceF.isFile()) {
			sourceFileList = new File[1];
			sourceFileList[0] = sourceF;
		}
		else
			sourceFileList = sourceF.listFiles();
		
		if(sourceFileList==null||sourceFileList.length==0)
			return false;
		
		if(!targetF.exists())
			makeDir(targetPath);
		
		for (File file : sourceFileList) {
			File temp = new File(targetF.getAbsolutePath()+File.separator+file.getName());
			
			if (file.isFile()) {
		   	    try {
			    	inChannel 	= new FileInputStream(file).getChannel();
			    	outChannel 	= new FileOutputStream(temp).getChannel();
			    	int 	maxCount 	= (64 * 1024 * 1024) - (32 * 1024);
			      	long 	size 			= inChannel.size();
			      	long 	position 	= 0;
			      	while (position < size) {
			        	position += inChannel.transferTo(position, maxCount, outChannel);
			  		}
			  		if( position >= size )	
			  			bRet = true;
			    } catch (Exception e) {
			    	e.printStackTrace();
			    	return false;
			    } finally {
			    	if( inChannel	!= null)	inChannel.close();	
					if( outChannel 	!= null)	outChannel.close();
			    }
		   } else {
			   folderCopy(file.getAbsolutePath(), temp.getAbsolutePath());
		   }
		}
		return bRet;
	} 
}

	 

