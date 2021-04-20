public class MyClass {
    public static String SOME_THING;

    public static Date getTodaysDate() {
        System.debug('hello from Apex');

        runBadCode();

        return System.today();
    }

    private static void runBadCode() {
        for (User user : [SELECT Id from User WHERE Id = :UserInfo.getUserId()]) {
            update user;
        }
    }
}