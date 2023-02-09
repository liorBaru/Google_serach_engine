package sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Scanner;

public class ReadFile {

    private String DirPath;
    private String[] Directories;
    private ArrayList<String> docList;
    //private int currentDirectory=0;
    private int count=0;

    private int currentDirectory=0;

    /**
     * 1
     * the class constractor, get the path to the corpus file
     * @param DirectoryPath
     */
    public ReadFile(String DirectoryPath){
        init(DirectoryPath);
    }
    public ReadFile(){
        docList = new ArrayList<String>();
    }
    /**
     * 1.1
     * initialize all the class data structures
     * @param DirectoryPath
     */
    public void init(String DirectoryPath){
        docList = new ArrayList<String>();
        Directories = DirectoryList(DirectoryPath);
        DirPath = DirectoryPath;
        currentDirectory=0;
    }
    /**
     * 1.1.1
     * returns a list of all the directories' names in the corpus directory
     * @param DirectoryPath
     * @return
     */
    private String[] DirectoryList(String DirectoryPath) {
        File file = new File(DirectoryPath);
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
        return directories;
    }

    /**
     * 2
     * return the next document in the corpus
     * @return
     */
    public String nextDocument(){
        if(docList.size()==0) {
            if (!generateNewList())
                return null;
        }
        String text="";
        if(docList.size()!=0) {
            text = docList.get(0);
            docList.remove(0);
        }
        if(text.equals(""))
            return null;
        return text;
    }

    /**
     * 3
     * check if there is more documents to read from the corous
     * @return
     */
    public boolean hasNextDocument(){
        if(docList.size()==0) {
            if (!generateNewList())
                return false;
        }
        return true;
    }

    /**
     * 2/3 .1
     * in case that there is no more document in the current file in the corpus,
     * its open the next file and create a new list of document
     * @return
     */
    private boolean generateNewList() {
        if(Directories==null/*||currentDirectory>Directories.length*/) {
            Directories = DirectoryList(DirPath);
        }
        String currentDoc="";
        BufferedReader br;
        try {
            br = new BufferedReader(
                    new FileReader(
                            new File(DirPath+"\\"
                                    +Directories[currentDirectory]
                                    +"\\"+Directories[currentDirectory])));
            String line;
            while((line = br.readLine()) != null) {
                currentDoc="";
                do {
                    line = br.readLine();
                    currentDoc += line+"*";
                } while (!line.equals("</DOC>"));
                docList.add(currentDoc);
            }
        } catch (Exception e) {
            //System.out.println(Directories[currentDirectory]+" "+ ++count +"  sample.ReadFile Class could not generate more docs \n"+e.toString());
            //currentDirectory--;
            count++;
        }
         currentDirectory++;
        //System.out.println(count + " " + Directories[currentDirectory]);
        return true;
    }
    /**
     * getter of the corpus path
     * @return
     */
    public String getDirPath() {
        return DirPath;
    }
    /**
     * setter for corpus path
     * @param dirPath
     */
    public void setDirPath(String dirPath) {
        DirPath = dirPath;
    }

}
