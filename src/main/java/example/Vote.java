package example;

import arc.util.Timer;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import java.util.ArrayList;

import static mindustry.Vars.playerGroup;

public class Vote {
    UnitFactory factory;
    Loadout loadout;
    Player player;

    String type;
    String unitType;

    int voteIdx=0;
    int bundletime = 0;
    int unitAmount=0;

    boolean isvoting = false;

    String[] bundlename = {"vote-50sec", "vote-40sec", "vote-30sec", "vote-20sec", "vote-10sec"};
    ArrayList<String> list = new ArrayList<>();
    int require;

    public Vote(UnitFactory factory,Loadout loadout) {
        this.factory=factory;
        this.loadout=loadout;
    }

    public boolean isIsvoting() {
        return isvoting;
    }

    void loadout_Vote(Player player, String type) {
        this.player = player;
        this.type = type;
        command();
    }

    void factory_Vote(Player player,String type,String unitType,int unitAmount) {
        this.player = player;
        this.type = type;
        this.unitType=unitType;
        this.unitAmount=unitAmount;
        command();
    }

    public boolean check(Player player){
        if(isvoting){
            player.sendMessage("[scarlet][Server][]vote-in-processing");
            return true;
        }if(MyPlugin.pending_gameover){
            player.sendMessage("[scarlet][Server][]Game is over. Please wait!");
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
                    Call.sendMessage("[scarlet][Server][]vote-launch to core-done");
                    loadout.use_loadout(player);
                } else {
                    Call.sendMessage("[scarlet][Server][]vote-launch to core-fail");
                }
                break;
            case "fill":
                if (require <= 0) {
                    Call.sendMessage("[scarlet][Server][]vote-launch to loadout-done");
                    loadout.use_loadout(player);
                } else {
                    Call.sendMessage("[scarlet][Server][]vote-launch to loadout-fail");
                }
                break;
            case "release":
                if (require <= 0) {
                    Call.sendMessage("[scarlet][Server][]vote-launch of units-done");
                    factory.send_units(player,unitType);
                } else {
                    Call.sendMessage("[scarlet][Server][]vote-launch of units-fail");
                }
                break;
            case "build":
                if (require <= 0) {
                    Call.sendMessage("[scarlet][Server][]vote-build "+unitType+"-done");
                    factory.build_unit(unitType,unitAmount);
                } else {
                    Call.sendMessage("[scarlet][Server][]vote-build "+unitType+"-fail");
                }
                break;

        }
        list.clear();
    }

    public void command() {
            list.clear();
            isvoting = true;
            if(playerGroup.size()==1){
                require=1;
            }else if (playerGroup.size() <= 3) {

                require = 2;
            } else {
                require = (int) Math.ceil((double) playerGroup.size() / 2);
            }
            String message = loadout.launch_amount + " " + (loadout.launch_item == null ? " of every resource" : loadout.launch_item.name);
        switch (type) {
            case "use":
                Call.sendMessage("[scarlet][Server][][orange] " +player.name+ "[] has casted vote to launch [orange]" + message + "[] to core. Open chat window and say [orange]'y' [white]to agree.");
                break;
            case "fill":
                Call.sendMessage("[scarlet][Server][][orange] " +player.name+ "[] has casted vote to launch [orange]" + message + "[] to loadout. Open chat window and say [orange]'y' [white]to agree.");
                break;
            case "build":
                Call.sendMessage("[scarlet][Server][][orange] " +player.name+ "[] has casted vote to build [orange]" +unitAmount+" "+unitType+ "[]. Open chat window and say [orange]'y' [white]to agree.");
                break;
            case "release":
                Call.sendMessage("[scarlet][Server][][orange] " +player.name+ "[] has casted vote to launch [orange]" +unitType+ "[] units. Open chat window and say [orange]'y' [white]to agree.");
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
                Call.sendMessage("[scarlet][Server][]"+bundlename[bundletime]);
                bundletime++;
                alert(idx);
            }
        },10);

    }

    public void add_vote(Player player, int vote) {
        if (list.contains(player.uuid)) {
            player.sendMessage("[scarlet][Server][]You already voted,sit down!");
            return;
        }
        require -= vote;
        list.add(player.uuid);
        if (require <= 0) {
            cancel();
        } else {
            Call.sendMessage("[scarlet][Server][][orange]" + require + " [white]more votes needed.");
        }
    }
}
