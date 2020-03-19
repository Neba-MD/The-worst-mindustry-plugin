package example;

import arc.util.Timer;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import java.util.ArrayList;
import java.util.TimerTask;

import static mindustry.Vars.playerGroup;

public class Vote {
    ExamplePlugin plugin;
    UnitFactory factory;
    Player player;

    String type;
    String unitType;

    int voteIdx=0;
    int bundletime = 0;

    boolean isvoting = false;
    boolean interrupted=false;

    String[] bundlename = {"vote-50sec", "vote-40sec", "vote-30sec", "vote-20sec", "vote-10sec"};
    ArrayList<String> list = new ArrayList<>();
    int require;

    public Vote(ExamplePlugin plugin,UnitFactory factory) {
        this.plugin = plugin;
        this.factory=factory;
    }

    public boolean isIsvoting() {
        return isvoting;
    }

    void launch_Vote(Player player, String type) {
        if(check()){return;}
        this.player = player;
        this.type = type;
        command();
    }
    void build_Vote(Player player,String type,String unitType) {
        if(check()){return;}
        this.player = player;
        this.type = type;
        this.unitType=unitType;
        command();
    }
    public boolean check(){
        if(isvoting){
            player.sendMessage("vote-in-processing");
            return true;
        }
        return false;
    }


    public void cancel() {
        voteIdx+=1;
        voteIdx%=1000;
        isvoting = false;
        bundletime = 0;
        switch (type) {
            case "use":
                if (require <= 0) {
                    Call.sendMessage("vote-launch to core-done");
                    plugin.use_layout(player);
                } else {
                    Call.sendMessage("vote-launch to core-fail");
                }
                break;
            case "fill":
                if (require <= 0) {
                    Call.sendMessage("vote-launch to layout-done");
                    plugin.use_layout(player);
                } else {
                    Call.sendMessage("vote-launch to layout-fail");
                }
                break;
            case "release":
                if (require <= 0) {
                    Call.sendMessage("vote-launch of units-done");
                    factory.send_units(player,unitType);
                } else {
                    Call.sendMessage("vote-launch of units-fail");
                }
                break;
            case "build":
                if (require <= 0) {
                    Call.sendMessage("vote-build "+unitType+"-done");
                    factory.build_unit(player,unitType);
                } else {
                    Call.sendMessage("vote-build "+unitType+"-fail");
                }
                break;

        }
        list.clear();
    }

    public void command() {
            isvoting = true;
            if(playerGroup.size()==1){
                require=1;
            }else if (playerGroup.size() <= 3) {

                require = 2;
            } else {
                require = (int) Math.ceil((double) playerGroup.size() / 2);
            }
            String message = Integer.toString(plugin.launch_amount) + " " + (plugin.launch_item == null ? " of every resource" : plugin.launch_item.name);
            switch (type) {
                case "use":
                    Call.sendMessage("vote-to launch [orange]" + message + "[white] to core.Open chat window and sey [orange]'y' [white]to agree.");
                    break;
                case "fill":
                    Call.sendMessage("vote-to launch [orange]" + message + "[white] to loadout.Open chat window and sey [orange]'y' [white]to agree.");
                    break;
                case "build":
                    Call.sendMessage("vote-to build [orange]" +unitType+ "[white].Open chat window and sey [orange]'y' [white]to agree.");
                    break;
                case "release":
                    Call.sendMessage("vote-to launch [orange]" +unitType+ "[white] units.Open chat window and sey [orange]'y' [white]to agree.");
                    break;
            }

            countdown(voteIdx);
            alert(voteIdx);
    }
    public void countdown(int idx){
        Timer.schedule(()->{
            if (idx!=voteIdx){
                return;
            }
            if(isIsvoting()){
                cancel();
            }
        },60);
    }
    public void alert(int idx){
        Timer.schedule(()->{
            if (idx!=voteIdx){
                return;
            }
            if (bundletime <= 4) {
                Call.sendMessage(bundlename[bundletime]);
                bundletime++;
                alert(idx);
            }
        },10);

    }

    public void add_vote(Player player, int vote) {
        if (list.contains(player.uuid)) {
            player.sendMessage("You already voted,sit down!");
            return;
        }
        require -= vote;
        list.add(player.uuid);
        if (require <= 0) {
            cancel();
        } else {
            Call.sendMessage("[orange]" + Integer.toString(require) + " [white]more votes needet.");
        }
    }
    public void interrupted() {
       interrupted = true;
    }
}
