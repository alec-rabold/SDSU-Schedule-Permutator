import java.util.*; 

public class Controller {
    public static void main(String[] args) throws Exception {

        /**
         * This program scrapes all classes available on SDSU's registration page in real-time. All of the data available on the registration
         * page is stored in nested Maps and Lists. Then, the program takes in any number of courses from the user and creates possible permutations (no repeats)
         * for that specific schedule. The program maps each course's times to days and checks for collisions through extensive bit manipulation. Some courses
         * have multiple meetings times/days (ex. Lecture and Lab), which has been accounted for. 
         * 
         * USAGE DEFINED BELOW. Use setTerm("Spring", 2018) to get most current registration data.
         *      Recommended to run via command line:
         *          Add Java SDK to compiler classpath then execute the following:
         *          javac Controller.java DepartmentScraper.java RegistrationScraper.java Options.java
         *          java Controller DepartmentScraper RegistrationScraper Options
         *
         * Next version: Iterate only through user-specified departments to drastically speed up load time.
         *
         * Possible bugs - Scraping current registration may occassionally come back with a nullPointerException since there are still null times, professors, etc.
         * in WebPortal's registration [rare]
         * 
         * Thanks for checking it out!
         * Alec
         * 
         */
        
        Options custom = new Options();

        /** Custom Search Parameters Go Here [Reference Options.java] */
        custom.setTerm("Spring", "2018");

        /** ITERATIONS */
        /** Un-comment following line to statically iterate ALL departments */
        // custom.iterateAll(); 
        /** Un-comment the following two lines for statically iterating one department */
        // custom.setDepartment("CS");
        // custom.iterateOne();

        //** COURSES ANALYZE */
        Scanner scan = new Scanner(System.in);

        System.out.println();
        System.out.println("Type in classes, delimited by 'enter', q to finish");
        System.out.println("     (ex: CS-107 [enter] CS-310 [enter] MATH-150 [enter] q [enter])");

        // Takes in the user's courses
        List<String> classes = new ArrayList<String>();
        String input = scan.nextLine();
        while(!input.equals("q")){
            classes.add(input);
            input = scan.nextLine();
        }

        custom.iterateInput(classes);

        // Begins the analyzation process
        custom.analyzePermutations(classes);
        
        /** Printing the Results (TESTING PURPOSES ONLY) */
        // custom.printHashMap();  

        /** Prints a list of available methods and their documentation- remove if desired */
        /*
        System.out.println();
        System.out.println("Here is a list of available commands to add to the main method in the Controller class");
        System.out.println();
        Thread.sleep(5000);
        custom.printOptions();
        */
        
    }
}