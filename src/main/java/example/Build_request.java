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

    String unitName;
    UnitFactory factory;

    public Build_request(String unitName,int time,UnitFactory factory){
        this.unitName=unitName;
        this.time=time;

        this.factory=factory;
        start_countdown();
    }
    public boolean info(Player player){
        if(interrupted){
            return false;
        }
        if (building){
            player.sendMessage("Factory is currently building [orange]" + unitName + "[white].It will be finished in " +
                    time / 60 + "min" + time % 60 + "sec.");
            return true;
        }
        return false;
    }

    private void start_countdown() {
        building=true;
        Call.sendMessage("[green]Building of " + unitName + " just started.It will take " + time / 60 + "min.");
        TimerTask countdown=new TimerTask() {
            @Override
            public void run() {
                time--;
            }
        };
        Timer.schedule(countdown,0,1,time-1);
        Timer.schedule(() -> {
            if (interrupted) {
                factory.requests.remove(this);
                return;
            }

            building = false;
            Call.sendMessage("[green]" + unitName + " is finished and waiting in a hangar.You can use factory egan.");
            factory.unitStats.get(unitName)[UnitFactory.unitCount]+=1;
        }, time);
    }
}
