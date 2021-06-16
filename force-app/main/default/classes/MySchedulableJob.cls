global class MySchedulableJob implements Schedulable {
    global void execute(SchedulableContext SC) {
        System.debug('running schedulable job');
    }
}