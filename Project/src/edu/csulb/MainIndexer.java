/*
 * To change this license header, choose Licensex Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*

package edu.csulb;

import cecs429.TermFrequency.*;
import cecs429.documents.DirectoryCorpus;
import cecs429.documents.DocumentCorpus;
import cecs429.index.*;
import cecs429.query.BooleanQueryParser;
import cecs429.query.QueryComponent;
import cecs429.query.RankedQueryParser;
import cecs429.text.BetterTokenProcessor;
import cecs429.text.TokenProcessor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.Integer.min;

///Users/indumanimaran/Documents/SET/MobyDick10Chapters
///Users/indumanimaran/Documents/SET/Test/
public class MainIndexer {

    private  static  ArrayList<String> modes;
    private static  ArrayList<TermFrequencyStrategy> rankRetrievalStrategy;

    private static DocumentCorpus newCorpus(Path directoryPath, String extension) {
        DocumentCorpus corpus;
        if (extension.contains("txt")) {
            corpus = DirectoryCorpus.loadTextDirectory(directoryPath, extension);
        } else {
            corpus = DirectoryCorpus.loadJsonDirectory(directoryPath, extension);
        }
        return corpus;
    }

    private static Index newIndex(DocumentCorpus corpus,DiskIndexWriter diskIndexWriter,Path directoryPath) {
        final long startTime = System.currentTimeMillis();
        Index index= diskIndexWriter.indexCorpus(corpus,directoryPath);

        final long endTime = System.currentTimeMillis();
        long indexTime = endTime - startTime;
        System.out.println("Time taken for indexing corpus:"+ indexTime /1000 +" seconds");
        return index;

    }

    public static void main(String[] args) throws FileNotFoundException {
        Scanner scanner = new Scanner(System.in);
        System.out.println(
                "1. Milestone 1 (Boolean queries, in-memory index)\n" +
                "2. Milestone 2 (Boolean and ranked queries, on-disk index)\n" +
                "3. Milestone 3 (Average Precision calculations)"
        );
        int MilestoneChoice = scanner.nextInt();

        getModesList();

        SnowballStemmer stemmer = new englishStemmer();

        System.out.println("1. Build Index");
        System.out.println("2. Query Index");

        int indexOperation = scanner.nextInt();

        System.out.println("Enter corpus path:");
        String PATH = scanner.next();
        // String PATH = "/Users/indumanimaran/Documents/SET/Test/";

        Path directoryPath = Paths.get(PATH);
        String sPath = directoryPath.toString();

        File folderfiles = new File(PATH);
        File[] listOfFiles = folderfiles.listFiles();
        String EXTENSION = FilenameUtils.getExtension(Objects.requireNonNull(listOfFiles)[0].getName());
        DocumentCorpus corpus = newCorpus(directoryPath, "." + EXTENSION);

        Index index = null;
        DiskIndexWriter diskIndexWriter = null;
        if (indexOperation == 1) {

            //System.out.println("EXTENSION is "+ EXTENSION);
            diskIndexWriter = new DiskIndexWriter();
            System.out.println("Indexing..." + sPath);
            index = newIndex(corpus, diskIndexWriter, directoryPath);

        } else {
            getRankRetrivalStrategy(sPath);

            //TO DO -- for milestone option
            index = new DiskPositionalIndex(directoryPath,corpus.getCorpusSize());
            String query;
            Scanner reader = new Scanner(System.in);
            OUTER:
            while (true) {
                System.out.print("Query : ");
                query = reader.nextLine();
                int len = query.split("\\s+").length;
                if (len == 1) {
                    switch (query) {
                        case ":q":
                            break OUTER;
                        case ":vocab":
                            System.out.println("@vocabulary ");
                            List<String> vocabulary = null;
                            try {
                                vocabulary = index.getVocabulary();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            List<String> newList = new ArrayList<>(vocabulary.subList(0, min(vocabulary.size(), 1000)));
                            newList.forEach(System.out::println);
                            System.out.println("#vocabulary terms: " + vocabulary.size());
                            break;

                        case ":biwordVocab":
                            Index biwordIndex = BiwordIndex.getIndex();
                            try {
                                System.out.println("Biword Index size: " + biwordIndex.getVocabulary().size());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        default:
                            queryPosting(corpus, index, query, directoryPath);
                            break;
                    }
                } else if (len == 2) {
                    String firstTerm = query.split("\\s+")[0];
                    switch (firstTerm) {
                        case ":stem":

                            String input = query.split("\\s+")[1];
                            stemmer.setCurrent(input); //set string you need to stem
                            stemmer.stem();  //stem the word
                            System.out.println(stemmer.getCurrent());
                            break;
                        case ":index":
                            System.out.println("Program Restarted");
                            Path tempPath = Paths.get(query.split("\\s+")[1]);


                            if (Files.exists(tempPath)) {
                                File folder = new File(query.split("\\s+")[1]);
                                File[] listOfFiles2 = folder.listFiles();
                                EXTENSION = FilenameUtils.getExtension(Objects.requireNonNull(listOfFiles2)[0].getName());
                                corpus = newCorpus(tempPath, "." + EXTENSION);
                                index = newIndex(corpus, diskIndexWriter, directoryPath);
                            } else {
                                System.out.println("The specified directory does not exist");
                            }
                            break;
                        default:
                            queryPosting(corpus, index, query, directoryPath);
                            break;
                    }
                } else {
                    queryPosting(corpus, index, query, directoryPath);
                    break;
                }
            }
        }
    }
    private static void getRankRetrivalStrategy(String sPath){

        rankRetrievalStrategy = new ArrayList<>();
        rankRetrievalStrategy.add(new DefaultFrequencyStrategy(sPath));
        rankRetrievalStrategy.add(new TfIdfStrategy(sPath));
        rankRetrievalStrategy.add(new OkapiStrategy(sPath));
        rankRetrievalStrategy.add(new WackyStrategy(sPath));

    }

    private static void getModesList(){
        modes = new ArrayList<>();
        modes.add("");
        modes.add("boolean");
        modes.add("ranked");
    }
    private static void queryPosting( DocumentCorpus corpus, Index index, String query,Path path)  {
        Scanner scanner = new Scanner(System.in);
        String reply = "y";
        String docName;
        int docId;
        String mode;

        TokenProcessor processor =new BetterTokenProcessor();
        ContextStrategy strategy = new ContextStrategy(rankRetrievalStrategy.get(0));

        System.out.println("Enter retrieval mode: 1.Boolean 2.Ranked");
        mode = scanner.next();
        mode = modes.get(Integer.parseInt(mode));

        if(mode.equalsIgnoreCase("2")){
            System.out.println("1.Default\n" +
                    "2.tf-idf\n" +
                    "3.Okapi BM25\n" +
                    "4.Wacky\n" +
                    "Enter choice:\n");
            int strategyChoice = scanner.nextInt();
            strategy= new ContextStrategy(rankRetrievalStrategy.get(strategyChoice));
        }

        QueryComponent queryComponent;
        if (mode.equalsIgnoreCase("boolean")){
            queryComponent = new BooleanQueryParser().parseQuery(query, processor);
        }else{
            queryComponent = new RankedQueryParser(strategy,corpus.getCorpusSize()).parseQuery(query, processor);
        }

        List<Posting> postings = queryComponent.getPostings(index);

        if (postings != null) {
            for (Posting p : postings) {
                if (p.getDocumentId() >= 0) {
                    docId = p.getDocumentId();
                    System.out.println(p.getDocumentId() + " " +p.getPositions());
                    System.out.println(corpus.getDocument(docId).getmFileName());
                }
            }
            System.out.println(postings.size() + " document(s)");

            while (reply.equalsIgnoreCase("y")) {
                System.out.println("View document(y/n):");
                reply = scanner.nextLine();
                if (reply.equalsIgnoreCase("y")) {
                    System.out.println("Enter document name:");
                    docName = scanner.nextLine();
                    for (Posting p : postings) {
                        docId = p.getDocumentId();
                        if (corpus.getDocument(docId).getmFileName().equalsIgnoreCase(docName)) {
                            try {
                                System.out.println(IOUtils.toString(corpus.getDocument(docId).getContent()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }
}*/