import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * "In this lab you will simulate demand paging and see how the number of page faults 
 * depends on page size, program size, replacement algorithm, and job mix (job mix is 
 * defined below and includes locality and multiprogramming level)."
 * 
 * @author YashJalan
 * @since 11/28/16
 */

public class Paging {
	
    //finds the next reference for a given process
    private static int findNext(double A, double B, double C, int S, int word, Scanner readRandom) {
        
    	double y = readRandom.nextInt() / (Integer.MAX_VALUE + 1d);
        
        if (y < A) {
            word = (word + 1) % S;
        } else if (y < A + B) {
            word = (word - 5 + S) % S;
        } else if (y < A + B + C) { 
            word = (word + 4) % S;
        } else {word = readRandom.nextInt() % S;}
        
        return word;
    }
    
    //driver+pager
    public static void main (String[] args) throws IOException {     

        //driver
        int machineSize = Integer.parseInt(args[0]);
        int pageSize = Integer.parseInt(args[1]);
        int procSize = Integer.parseInt(args[2]);
        int job = Integer.parseInt(args[3]);
        int nRef = Integer.parseInt(args[4]);
        String alg = args[5];
        
        System.out.println("The machine size is " + machineSize +".");
        System.out.println("The page size is " + pageSize +".");
        System.out.println("The process size is " + procSize +".");
        System.out.println("The job mix number is " + job +".");
        System.out.println("The number of references per process is " + nRef +".");
        System.out.println("The replacement algorithm is " + alg +".\n");
        
        //no. of processes
        int nProc = 1; 
        if (job != 1)
        	nProc = 4;
        //holds the current reference for each process
        int[] word = new int[nProc];
        
        //frame table
        //each row represents a frame (ordered highest to lowest)
        //col0: process, col1: page table/frame no., col2: time stamp
        Integer[][] frameTable = new Integer[machineSize/pageSize][3]; 
        for (Integer[] row: frameTable)
            Arrays.fill(row, -1);
        
        int[] faults = new int[nProc];
        //when each frame was last referenced 
        int[] time = new int[machineSize/pageSize];
        int[] evictions = new int[nProc];
        double[] residence = new double[nProc];
        
        //remaining references 
        int[] remRefs = new int[nProc];
        Arrays.fill(remRefs, nRef);
        
        int counter;
               
        //filling in starting words for each process
        for (counter = 0; counter < nProc; counter++) {
        	word[counter] = (111 * (counter + 1)) % procSize;
        }        
        
        //each entry in the list represents a process
        //each entry in the array represents A,B,C based on job mix
        ArrayList<double[]> prob = new ArrayList<double[]>();
        if(job == 1) { 
        	prob.add(new double[]{1,0,0}); 
        } else if (job == 2) { 
            for(counter = 0; counter < nProc; counter++) {
                prob.add(new double[]{1,0,0}); 
            }
        } else if (job == 3) { 
            for(counter = 0; counter < nProc; counter++) {
                prob.add(new double[]{0,0,0}); 
            }
        } else if (job == 4) { 
            prob.add(new double[]{0.75,0.25,0}); 
            prob.add(new double[]{0.75,0,0.25});
            prob.add(new double[]{0.75,0.125,0.125}); 
            prob.add(new double[]{0.5,0.125,0.125}); 
        }
        
        File rand = new File("random-numbers.txt");
        Scanner readRandom = new Scanner(rand);
        
        int free; //index of first free frame
        int evict = -1; //index of frame to evict
        int match; //index of matched frame
        int process = 0;
        
        //pager
        for(counter = 1; counter < nRef * nProc + 1; counter++) {
            
        	//find matching frame
            match = -1;
            for (int i = 0; i < frameTable.length; i++) {
                if (frameTable[i][0] == process) 
                    if (frameTable[i][1] == word[process] / pageSize) {
                    	match = i; break;
                    }
            }
            
            //hit
            if(match != -1) {
                frameTable[match][2] = counter; 
            } else { //page fault
                faults[process]++;
                free = -1;
                //finding free frame
                for (int i = 0; i < frameTable.length; i++) {
                        if (frameTable[i][1] == -1) {
                        	free = i; break;
                        }
                }
                //not found, must replace using alg.
                if (free == -1) {
                    if (alg.equals("lru")) {
                        evict = 0;
                        //finding least recently referenced frame
                        for(int i = 1; i < frameTable.length; i++) 
                            if (frameTable[evict][2] > frameTable[i][2])
                            	evict = i;
                        
                    } else if (alg.equals("random")) {
                        evict = (readRandom.nextInt() + 1) % frameTable.length;
                    } else if (alg.equals("lifo")) {
                        evict = frameTable.length - 1;
                    } else {
                    	System.out.println("Error: incorrect replacement algorithm");
                    	return;
                    }
                    
                    //update variables
                    residence[frameTable[evict][0]] += (counter - time[evict]);
                    evictions[frameTable[evict][0]]++;
                    frameTable[evict][0] = process; 
                    frameTable[evict][1] = word[process] / pageSize; 
                    frameTable[evict][2] = counter;   
                    time[evict] = counter;
                } else {
                	//free frame updates
                    frameTable[free][0] = process; 
                    frameTable[free][1] = word[process] / pageSize; 
                    frameTable[free][2] = counter; 
                    time[free] = counter;
                }
            }
            
            //find next reference and process
            word[process] = findNext(prob.get(process)[0], prob.get(process)[1], prob.get(process)[2], procSize, word[process], readRandom);
            remRefs[process]--;
            if (remRefs[process] == 0) {
                process++; 
            } else if(counter % 3 == 0 && remRefs[process] > nRef % 3 - 1) { //increment process if q==3 and at least 3 remRefs remain
                process = (process + 1) % nProc; 
            }
        }
        
        readRandom.close();
        
        //display final results
        for (int i = 0; i < faults.length; i++) {
            System.out.print("Process " + (i+1) + " had " + faults[i] + " faults and its average residency is ");
            if (evictions[i] == 0)
                System.out.println("undefined.");
            else 
                System.out.println((residence[i]/evictions[i]) + ".");
        }
        
        //Total results/sums
        int sum = 0;
        double evictSum = 0;
        double resSum = 0;
        for(int i = 0; i < nProc; i++) {
            sum += faults[i];
            evictSum += evictions[i];
            resSum += residence[i];
        }
        System.out.print("\nThe total number of faults is " + sum + " and the overall average residency is ");
        if(evictSum == 0)
            System.out.println("undefined.");
        else 
            System.out.println((resSum/evictSum) + ".");
    }
}
