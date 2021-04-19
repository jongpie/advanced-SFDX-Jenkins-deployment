@isTest
private class MyClass_Tests {

    @isTest
    private static void it_should_call_method() {
        Date returnedDate = MyClass.getTodaysDate();

        System.assertEquals(System.today(), returnedDate);
    }
}