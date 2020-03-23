package example;

import arc.util.Timer;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.Player;
import mindustry.gen.Call;

import java.util.TimerTask;

public class Build_request{
    public boolean building=false;
    public boolean interrupted=false;

    public int time;
    public int amount;

    String unitName;
    UnitFactory factory;

    public Build_request(String unitName,int time,int amount,UnitFactory factory){
        this.unitName=unitName;
        this.time=time;
        this.factory=factory;
        this.amount=amount;
        start_countdown();
    }

    public String info(){
        if (building){
            return "[scarlet][Server][]Factory is currently building [orange]" +amount+" "+ unitName +
                    "[white].It will be finished in " +
                    time / 60 + "min" + time % 60 + "sec.\n";
        }
        return null;
    }

    private void start_countdown() {
        building=true;
        Call.sendMessage("[scarlet][Server][][green]Building of " +amount+" "+ unitName +
                " just started.It will take " + time / 60 + "min.");
        TimerTask countdown=new TimerTask() {
            @Override
            public void run() {
                time--;
            }
        };
        Timer.schedule(countdown,0,1,time-1);
        Timer.schedule(() -> {
            building = false;
            Call.sendMessage("[scarlet][Server][][green]" +amount+" "+ unitName +
                    " is finished and waiting in a hangar.You can use factory egan.");
            factory.unitStats.get(unitName)[UnitFactory.unitCount]+=amount;
        }, time);
    }
}
