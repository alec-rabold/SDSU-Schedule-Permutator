import java.net.*;
import java.util.*;
import java.io.*;

public class RegistrationScraper {
    final String REGISTRATION_SEARCH_PAGE = "https://sunspot.sdsu.edu/schedule/search?mode=search";
    protected Map<String, List<Course>> departments = new TreeMap<String, List<Course>>();
    private URL Registration_URL;
    protected String parameters;
    protected boolean departmentSet = false;
    protected String departmentSingle = "";
    private int sectionMeetingCounter = 0;

    protected class Course {
        String courseID;
        String schedNumber;
        String title;
        String units;
        List<String> times = new ArrayList<String>();
        List<String> instructors = new ArrayList<String>();
        List<String> days = new ArrayList<String>();
        public boolean isComplete() {
            return (courseID != null && schedNumber != null && title != null && units != null &&
                    !times.isEmpty() && !instructors.isEmpty() && !days.isEmpty());
        }
    }

    public String getParameters() {
        return this.parameters;
    }

    public void iterateOne() throws Exception {
        if(departmentSet) {
            this.parseDepartmentHTML(departmentSingle);
        }
        else {
            System.out.println("ERROR: You have not set the department you want to scrape.");
            System.out.println("Type 'HELP' for a list of options and commands.");
        }
    }

    public void iterateAll() throws Exception {

        DepartmentScraper departments = new DepartmentScraper();

        System.out.println("Scraping...");

        String department = "";
        for(Map.Entry<String, String> entry : departments.getDepartmentMap().entrySet() ) {
            department = entry.getKey();
            this.parseDepartmentHTML(department);
        }
        System.out.println();
    }

    public void iterateInput(List<String> inputList) throws Exception {

        Set<String> departments = new HashSet<String>();
        System.out.println("Scraping...");
        for(String courseName : inputList) {
            String dept = courseName.substring(0, courseName.indexOf("-"));
            departments.add(dept);
        }
        for(String dept : departments) {
            this.parseDepartmentHTML(dept);
        }
        System.out.println();
    }

    public void parseDepartmentHTML(String department) throws Exception {

        setDepartmentSearch(department);

        Course temp = new Course();
        List<Course> tempList = new ArrayList<Course>();

        System.out.println(Registration_URL.toString());

        
        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(Registration_URL.openStream()));
            
            String inputLine, value;

            while((inputLine = in.readLine()) != null) {

                updateCount(inputLine);

                
                if(inputLine.contains("<a href=\"sectiondetails") && !inputLine.contains("footnote")) {
                    // check that class has all attributes, then add the course & reset it
                    // I put it here so that it only adds the course once it finds the next one
                    if(temp.isComplete()) {
                        tempList.add(temp);
                        temp = new Course();
                    }
                    else {
                        temp = new Course();
                    }

                    int indexStart = inputLine.indexOf("\">") + 2;
                    int indexEnd = inputLine.indexOf("</a>");
                    value = inputLine.substring(indexStart, indexEnd);
                    temp.courseID = value;
                }

                else if(inputLine.contains("sectionFieldSec")) {
                    value = parseSection(inputLine);
                    //if(!value.equals("Sec"))
                        // this.section.add(value);
                }

                else if(inputLine.contains("sectionFieldSched")) {
                    value = parseSection(inputLine);
                    if(!value.equals("Sched #") && value.matches(".*\\d+.*"))
                        temp.schedNumber = value;
                        //this.scheduleNumber.add(value);
                }

                else if(inputLine.contains("sectionFieldTitle")) {
                    value = parseSection(inputLine);
                    if(!value.equals("Course Title") && value.matches(".*[a-zA-Z]+.*"))   
                        temp.title = value;
                        //this.courseTitle.add(value);
                }
                else if(inputLine.contains("sectionFieldUnits")) {
                    value = parseSection(inputLine);
                    if(!value.equals("Units") && value.matches(".*\\d+.*"))
                        temp.units = value;
                        //this.units.add(value);
                }
                else if(inputLine.contains("sectionFieldType")) {
                    value = parseSection(inputLine);
                    //if(!value.equals("Format"))
                        // this.format.add(value);
                }
                else if(inputLine.contains("sectionFieldTime")) {
                    value = parseSection(inputLine);
                    if((!value.equals("Time")) && value.matches(".*\\d+.*")){
                        temp.times.add(value);
                    }
                }
                else if(inputLine.contains("sectionFieldDay")) {
                    value = parseSection(inputLine);
                    if(!value.equals("Day") && value.matches(".*[a-zA-Z]+.*") && !value.equals("Arranged"))
                        temp.days.add(value);
                        //this.day.add(value);
                }
                else if(inputLine.contains("sectionFieldLocation") && !inputLine.contains(">Location<")) {
                    if((inputLine = in.readLine()).contains("<a")){
                        updateCount(inputLine);
                        inputLine = in.readLine(); // the HTML data is on the 3rd line due to inconsistent formatting on WebPortal
                        updateCount(inputLine);
                        value = inputLine.trim();
                        // this.location.add(value);
                    }
                    else {
                        inputLine = in.readLine();
                        updateCount(inputLine);
                        value = inputLine.trim();
                        // this.location.add(value);
                        
                    }
                }
                else if(inputLine.contains("sectionFieldSeats") && !inputLine.contains(">Seats Open<")) {
                    boolean seatsFound = false;
                    while(!seatsFound) {
                        inputLine = in.readLine();
                        if(inputLine.contains("Waitlisted")) {
                            inputLine = inputLine.trim();
                            int indexStart = 0;
                            int indexEnd = inputLine.indexOf("<br>");
                            value = inputLine.substring(indexStart, indexEnd);
                            // this.seatsOpen.add(value);
                            seatsFound = true;
                        }
                        else if(inputLine.contains("/") && !(inputLine.contains("<"))) {
                            value = inputLine.trim();
                            // this.seatsOpen.add(value);
                            seatsFound = true;
                        }
                    }
                }
                
                else if(inputLine.contains("sectionFieldInstructor") && !inputLine.contains(">Instructor<")) {
                    for(int i = 0; i < 3; i++) {
                        inputLine = in.readLine();
                        updateCount(inputLine);
                        if(inputLine.contains("<a href=\"search?mode=search&instructor")) {    
                            int indexStart = inputLine.indexOf("\">") + 2;
                            int indexEnd = inputLine.indexOf("</a>");
                            value = inputLine.substring(indexStart, indexEnd);
                            if(!value.equals("Instructor") && value.matches(".*[a-zA-Z]+.*")) { 
                                //this.instructor.add(value);
                                temp.instructors.add(value);
                            }
                        }
                    }
                }
            }
        }
        catch(NullPointerException e) {
            System.out.print("  <-- No courses for this department in selected period");
        }

        departments.put(department, tempList);
    }


    public void updateCount(String inputLine) {

        // Accounts for courses with a Lecture and Activity class
        if(inputLine.contains("sectionRecordEven") || inputLine.contains("sectionRecordOdd")) {
            this.sectionMeetingCounter = 0;
        }   
        
        // Handles multiple locations, teachers, times, etc. per one class
        if(inputLine.contains("sectionMeeting")) { 
            this.sectionMeetingCounter++;
            if((this.sectionMeetingCounter) >= 2) {
                // this.course.add("");
                // this.seatsOpen.add("");
            }
        }     
    }

    public String parseSection(String inputLine) {
        int indexStart = inputLine.indexOf("column\">") + 8;
        int indexEnd = inputLine.indexOf("</div>");
        String value = inputLine.substring(indexStart, indexEnd);
        return value;
    }

    public void setDepartmentSearch() throws MalformedURLException {
        String formURL = "";
        if(this.parameters != null) 
            formURL = REGISTRATION_SEARCH_PAGE + parameters;
        else 
            formURL = REGISTRATION_SEARCH_PAGE;
        String searchURL = formatURL(formURL);
        this.Registration_URL = new URL(formURL);
    }
    public void setDepartmentSearch(String department) throws MalformedURLException {
        String formURL = "";
        if(this.parameters != null) 
            formURL = REGISTRATION_SEARCH_PAGE + "&abbrev=" + department + parameters;
        else 
            formURL = REGISTRATION_SEARCH_PAGE + "&abbrev=" + department;
        String searchURL = formatURL(formURL);
        this.Registration_URL = new URL(searchURL);
    }

    public String formatURL(String url) {
        StringBuilder newURL = new StringBuilder();
        
        for(int i = 0; i < url.length(); i++) {
            if(url.charAt(i) == ' ')
                newURL.append('+');
            else 
                newURL.append(url.charAt(i));
        }
        return newURL.toString();
    }

    public void printHashMap() {
        for (String dept: departments.keySet()){

            List<Course> deptCourseList = departments.get(dept);
            System.out.println(dept + ":");
            for(Course crs : deptCourseList) {
                System.out.println("\t" + crs.courseID + " - " + crs.title);
                for(int k = 0; k < crs.times.size(); k++){
                    System.out.println("\t  Time: " + crs.times.get(k));
                    System.out.println("\t  Days: " + crs.days.get(k));
                    System.out.println("\t  Instructor: " + crs.instructors.get(k));
                }
            }
        }
    }
    public void analyzePermutations(List<String> chosenCourses){
        int i = 0;
        Map<Integer, List<Course>> possibleCourses = new TreeMap<Integer, List<Course>>();
        for(String course : chosenCourses) {
            List<Course> tempList = new ArrayList<Course>();
            String deptSubString = course.substring(0,course.indexOf("-"));
            for(String dept : departments.keySet()) {
                if(dept.equals(deptSubString)) {
                    List<Course> departmentCourses = departments.get(dept);
                    for(Course entry : departmentCourses) {
                        if(course.equals(entry.courseID)) tempList.add(entry);
                    }

                }
            } 
            possibleCourses.put(i, tempList);
            i++;
        }
        for (int j : possibleCourses.keySet()){

            List<Course> specificCourseList = possibleCourses.get(j);
            System.out.println("Class " + (j+1) + ": ");
            for(Course crs : specificCourseList) {
                System.out.println("\t" + crs.courseID + " - " + crs.title);
                System.out.println("\t      Schedule #: " + crs.schedNumber);
                System.out.println("\t      Units: " + crs.units);

                for(int k = 0; k < crs.instructors.size(); k++){
                    if(k < crs.instructors.size()) System.out.println("\t\t  Instructor: " + crs.instructors.get(k));
                    if(k < crs.times.size())System.out.println("\t\t  Time: " + crs.times.get(k));
                    if(k < crs.days.size())System.out.println("\t\t  Days: " + crs.days.get(k));
                    System.out.println();
                }
            }
        }
        permuteCourses(possibleCourses, chosenCourses.size());
    }

    // This is the bad boy doing all the bit manipulation
    public void permuteCourses(Map<Integer, List<Course>> possibleCourses, int numCourses){
        List<List<Course>> validSchedules = new ArrayList<List<Course>>();
        int[] iterationVariables = new int[numCourses];
        while(iterationVariables[0] < (possibleCourses.get(0).size())) {
            List<Course> temp = new ArrayList<Course>();
            boolean schedulesCollide = false;
            long[] combinedTimeBlocks = new long[6]; // 6 days
            iteratePerm: // label for for-loop (to be used with break down below)
            for(int i = numCourses - 1; i >= 0; i--) {
                temp.add(possibleCourses.get(i).get(iterationVariables[i]));
                // check days BEFORE times
                for(int multipleDaySlots = 0; multipleDaySlots < possibleCourses.get(i).get(iterationVariables[i]).days.size(); multipleDaySlots++) { // TOOK OUT -1
                    int[] daysArray = convDaysToArray(possibleCourses.get(i).get(iterationVariables[i]).days.get(multipleDaySlots));
                    for(int j = 0; j < daysArray.length; j++) {
                        if(daysArray[j] == 1) {
                            if((convertTimesToBits(possibleCourses.get(i).get(iterationVariables[i]).times.get(multipleDaySlots)) & combinedTimeBlocks[j]) != 0) {schedulesCollide = true; break iteratePerm;}
                            else{combinedTimeBlocks[j] = (convertTimesToBits(possibleCourses.get(i).get(iterationVariables[i]).times.get(multipleDaySlots)) | combinedTimeBlocks[j]);}
                        }
                    }
                }
            }
            if(!schedulesCollide) {
                validSchedules.add(temp);
            }

            // increment the variables up
            iterationVariables[numCourses - 1] += 1;
            for(int i = numCourses - 1; i > 0; i--) {
                if(iterationVariables[i] == possibleCourses.get(i).size()) {
                    iterationVariables[i] = 0;
                    iterationVariables[i-1] += 1;
                }
            }
        }
        // PRINTING
        if(validSchedules.size() > 200) {
            System.out.println("\nThere are " + validSchedules.size() + " possible permutations for your schedule. Do you still want to display them? (Y/N)");
            Scanner scan = new Scanner(System.in);
            String choice = scan.nextLine();
            if(choice.toUpperCase().equals("N")) System.exit(0);
        }
        for(List<Course> list : validSchedules) {
            System.out.println("VALID SCHEDULE:");
            System.out.println("-------------------");
            System.out.println();
            for(Course entry : list) {
                System.out.println("\t" + entry.courseID + ": " + entry.title);
                System.out.println("\t    Schedule #: " + entry.schedNumber);
                System.out.println();
                for(int k = 0; k < entry.instructors.size(); k++){
                    System.out.print("\t\t  Days: ");
                    if(k < entry.days.size()) System.out.println(entry.days.get(k));
                        else System.out.println();
                    System.out.print("\t\t  Time: ");
                    if(k < entry.times.size()) System.out.println(entry.times.get(k));
                        else System.out.println();
                    System.out.print("\t\t  Instructor: ");
                    if(k < entry.instructors.size()) System.out.println(entry.instructors.get(k));
                    System.out.println();
                }
            }
            
            System.out.println();
        }
        System.out.println("\nThere are " + validSchedules.size() + " possible permutations for your schedule!");
    }

    /**
    * This method will convert a String representation of a time-block to a binary
    * representation of 15min increments starting at 8:00am (800)
    * Ex: "800-850" --> 1111 (8:45, 8:30, 8:15, 8:00)
    *  "1030-1120" --> 0011 1100 0000 0000 (11:15, 11:00, 10:45, 10:30)
     */
    public long convertTimesToBits(String timeBlock) {
        String startTime = timeBlock.substring(0, timeBlock.indexOf("-"));
        String endTime = timeBlock.substring(timeBlock.indexOf("-") + 1);
        // 8:00 offset (800 becomes 000) ==> multiplier: 8 --> 0 
        // divide by 2 for three vs four digit numbers
        int startTimeHundreds = Integer.parseInt(startTime.substring(0,(startTime.length()/2)));
        int endTimeHundreds = Integer.parseInt(endTime.substring(0,(endTime.length()/2)));
        // spot in bundle of 4 bits (00, 15, 30, 45) --> (0, 1, 2, or 3)
        int startTimeTens = Integer.parseInt(startTime.substring(startTime.length()-2));
        int endTimeTens = Integer.parseInt(endTime.substring(endTime.length()-2));
        // Construct the bit String; represents both time and length of time
        StringBuilder bitBuilder = new StringBuilder();
        int numOnes = (((endTimeTens - startTimeTens) < 0 ? endTimeTens-startTimeTens+60 : endTimeTens-startTimeTens)  / 15) + 1;
        int onesFourMultiplier = endTimeHundreds - startTimeHundreds;
        // check for hour cutoff (ex. 845-900) shouldn't have 6 ones, only 2
        if((endTimeTens - startTimeTens) < 0) onesFourMultiplier--;
        int numZeros = startTimeTens / 15;
        int zerosFourMultiplier = startTimeHundreds - 8;

        // Build the bits
        for(int i = 0; i < onesFourMultiplier; i++) bitBuilder.append("1111");
        for(int i = 0; i < numOnes; i++) bitBuilder.append("1");
        for(int i = 0; i < zerosFourMultiplier; i++) bitBuilder.append("0000");
        for(int i = 0; i < numZeros; i++) bitBuilder.append("0");

        String bitValue = bitBuilder.toString();
        return Long.parseLong(bitValue, 2);
    }
    

    // Binary representation of weekdays (checked -> 1, unchecked -> 0)
    public int[] convDaysToArray(String days) {
        int[] res = new int[6];
        if(days.contains("M")) res[0] = 1;
        if(days.contains("W")) res[2] = 1;
        if(days.contains("TH")) res[3] = 1;
        if(days.contains("F")) res[4] = 1;
        if(days.contains("S")) res[5] = 1;
        int tuesCount = 0;
        for(int i = 0; i < days.length(); i++) {if(days.charAt(i) == 'T') tuesCount++;}
        switch(tuesCount) {
            case 1:
                if(days.indexOf("T") == (days.length() - 1)) res[1] = 1; // MT, T
                else if(days.charAt(days.indexOf("T") + 1) != 'H') res[1] = 1; // MTW, TF, etc. 
                break;
            case 2: 
                res[1] = 1; // TTH, etc.
                break; 
        }
        return res;
    }
}
