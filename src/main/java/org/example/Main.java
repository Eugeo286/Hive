package org.example;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

/**
 * Console entry point for The Hive.
 * Use this to test your logic directly without a browser.
 * The Spring REST API (HiveApplication) is the primary interface.
 */
public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        UserDAO    userDAO    = new UserDAO();
        ContentDAO contentDAO = new ContentDAO();
        User currentUser = null;

        while (true) {
            System.out.println("\n========== THE HIVE ==========");
            if (currentUser != null)
                System.out.println("Logged in as: " + currentUser.getName()
                        + " [" + (currentUser instanceof Teacher ? "Teacher" : "Student") + "]");

            System.out.println(" 1. Register          2. Login");
            System.out.println(" 3. Post Question     4. Answer Question");
            System.out.println(" 5. View Questions    6. View Answers");
            System.out.println(" 7. Post Resource     8. View Resources by Course");
            System.out.println(" 9. Delete Resource  10. Post Assignment (Teacher)");
            System.out.println("11. Submit Assignment 12. View Assignment Answers");
            System.out.println("13. Schedule Meeting 14. View Meetings");
            System.out.println("15. Leaderboard      16. Subject Stats");
            System.out.println("17. Urgent Questions (Teacher) 18. Verify Answer (Teacher)");
            System.out.println("19. Delete Question  20. Logout");
            System.out.println(" 0. Exit");
            System.out.print("Select: ");

            int choice;
            try { choice = Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { continue; }

            switch (choice) {

                // ---- REGISTER ----
                case 1: {
                    System.out.print("Name: ");       String n = sc.nextLine();
                    System.out.print("Email: ");      String e = sc.nextLine();
                    System.out.print("Password: ");   String p = sc.nextLine();
                    System.out.print("1. Student  2. Teacher: ");
                    int r = Integer.parseInt(sc.nextLine().trim());
                    if (r == 2) userDAO.saveUser(new Teacher(n, e, "Staff"), p, "teacher");
                    else        userDAO.saveUser(new Student(n, e), p, "student");
                    break;
                }

                // ---- LOGIN ----
                case 2: {
                    System.out.print("Email: ");    String le = sc.nextLine();
                    System.out.print("Password: "); String lp = sc.nextLine();
                    currentUser = userDAO.login(le, lp);
                    if (currentUser != null) System.out.println("Welcome, " + currentUser.getName() + "!");
                    else                     System.out.println("Login failed.");
                    break;
                }

                // ---- POST QUESTION ----
                case 3: {
                    if (currentUser == null) { System.out.println("Please log in first."); break; }
                    System.out.print("Title: ");    String t = sc.nextLine();
                    System.out.print("Subject: ");  String s = sc.nextLine();
                    contentDAO.saveQuestion(new Question(t, s, "Medium", currentUser.getName()));
                    System.out.println("Question posted.");
                    break;
                }

                // ---- ANSWER QUESTION ----
                case 4: {
                    if (!(currentUser instanceof Student)) { System.out.println("Students only."); break; }
                    System.out.print("Question ID: "); int qid = Integer.parseInt(sc.nextLine().trim());
                    System.out.print("Your answer: "); String ans = sc.nextLine();
                    Question temp = new Question("", "", "", ""); temp.setId(qid);
                    ((Student) currentUser).answerQuestion(temp, ans, userDAO, contentDAO);
                    System.out.println("Answer posted. +10 points!");
                    break;
                }

                // ---- VIEW ALL QUESTIONS ----
                case 5: contentDAO.viewAllQuestions(); break;

                // ---- VIEW ANSWERS ----
                case 6: {
                    System.out.print("Question ID: ");
                    contentDAO.viewAnswersForQuestion(Integer.parseInt(sc.nextLine().trim()));
                    break;
                }

                // ---- POST RESOURCE ----
                case 7: {
                    if (currentUser == null) { System.out.println("Please log in first."); break; }
                    System.out.print("Title: ");         String rt = sc.nextLine();
                    System.out.print("Link/URL: ");      String rl = sc.nextLine();
                    System.out.print("Course: ");        String rc = sc.nextLine();
                    System.out.print("Type (lecture/book/link): "); String rtype = sc.nextLine();
                    contentDAO.saveResource(new Resource(rt, rl, rc, currentUser.getName(), rtype));
                    System.out.println("Resource posted.");
                    break;
                }

                // ---- VIEW RESOURCES ----
                case 8: {
                    System.out.print("Course: ");
                    List<Resource> resources = contentDAO.getResourcesByCourse(sc.nextLine());
                    resources.forEach(r -> System.out.println("[" + r.getResourceType() + "] "
                            + r.getTitle() + " → " + r.getLink()));
                    break;
                }

                // ---- DELETE RESOURCE ----
                case 9: {
                    if (currentUser == null) { System.out.println("Please log in first."); break; }
                    System.out.print("Resource ID to delete: ");
                    contentDAO.deleteResource(Integer.parseInt(sc.nextLine().trim()));
                    break;
                }

                // ---- POST ASSIGNMENT (Teacher only) ----
                case 10: {
                    if (!(currentUser instanceof Teacher)) { System.out.println("Teachers only."); break; }
                    System.out.print("Assignment Title: ");   String at = sc.nextLine();
                    System.out.print("Description: ");        String ad = sc.nextLine();
                    System.out.print("Course: ");             String ac = sc.nextLine();
                    System.out.print("Deadline (YYYY-MM-DDTHH:MM): "); String dl = sc.nextLine();
                    System.out.print("Answer key: ");         String ak = sc.nextLine();
                    contentDAO.saveAssignment(new Assignment(
                            at, ad, ac, currentUser.getName(),
                            LocalDateTime.parse(dl), ak));
                    System.out.println("Assignment posted.");
                    break;
                }

                // ---- SUBMIT ASSIGNMENT ----
                case 11: {
                    if (!(currentUser instanceof Student)) { System.out.println("Students only."); break; }
                    System.out.print("Assignment ID: "); int aid = Integer.parseInt(sc.nextLine().trim());
                    System.out.print("Your submission: "); String sub = sc.nextLine();
                    contentDAO.saveSubmission(aid, new Submission(aid, currentUser.getName(), sub));
                    break;
                }

                // ---- VIEW ASSIGNMENT ANSWERS ----
                case 12: {
                    System.out.print("Assignment ID: ");
                    System.out.println(contentDAO.getAnswerIfDeadlinePassed(
                            Integer.parseInt(sc.nextLine().trim())));
                    break;
                }

                // ---- SCHEDULE MEETING ----
                case 13: {
                    if (currentUser == null) { System.out.println("Please log in first."); break; }
                    System.out.print("Topic: ");           String vt = sc.nextLine();
                    System.out.print("Subject/Course: ");  String vs = sc.nextLine();
                    System.out.print("Date & Time (YYYY-MM-DDTHH:MM): "); String vtime = sc.nextLine();
                    System.out.print("Meeting Link: ");    String vl = sc.nextLine();
                    contentDAO.saveVideoSession(new VideoSession(vt, vs, currentUser.getName(), vtime, vl));
                    System.out.println("Meeting scheduled.");
                    break;
                }

                // ---- VIEW MEETINGS ----
                case 14: {
                    contentDAO.getAllVideoSessions().forEach(VideoSession::displaySession);
                    break;
                }

                // ---- LEADERBOARD ----
                case 15: {
                    userDAO.getLeaderboard(10).forEach(s ->
                            System.out.println(s.getName() + ": " + s.getPoints() + " pts"));
                    break;
                }

                // ---- SUBJECT STATS ----
                case 16: contentDAO.getSubjectStats(); break;

                // ---- URGENT QUESTIONS (Teacher) ----
                case 17: {
                    if (!(currentUser instanceof Teacher)) { System.out.println("Teachers only."); break; }
                    List<Question> urgent = contentDAO.getUnansweredOver24Hours();
                    if (urgent.isEmpty()) System.out.println("No urgent questions.");
                    else urgent.forEach(q -> System.out.println("[URGENT] [ID " + q.getId() + "] "
                            + q.getSubject() + " — " + q.getTitle()));
                    break;
                }

                // ---- VERIFY ANSWER (Teacher) ----
                case 18: {
                    if (!(currentUser instanceof Teacher)) { System.out.println("Teachers only."); break; }
                    System.out.print("Answer ID to verify: ");
                    contentDAO.verifyAnswer(Integer.parseInt(sc.nextLine().trim()));
                    System.out.println("Answer verified.");
                    break;
                }

                // ---- DELETE QUESTION (Teacher) ----
                case 19: {
                    if (!(currentUser instanceof Teacher)) { System.out.println("Teachers only."); break; }
                    System.out.print("Question ID to delete: ");
                    contentDAO.deleteQuestion(Integer.parseInt(sc.nextLine().trim()));
                    break;
                }

                // ---- LOGOUT ----
                case 20: currentUser = null; System.out.println("Logged out."); break;

                // ---- EXIT ----
                case 0: System.out.println("Goodbye!"); System.exit(0);
            }
        }
    }
}
