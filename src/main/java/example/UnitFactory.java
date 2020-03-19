
package example;


import arc.util.Time;
import com.sun.org.apache.regexp.internal.RE;
import mindustry.content.UnitTypes;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.type.Item;
import static mindustry.Vars.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;

import arc.util.Timer;


public class UnitFactory {
    boolean traveling=false;
    boolean interrupted=false;
    private final String ERAD="eradicator";
    private final String LICH="lich";
    private final String REAP="reaper";
    ExamplePlugin plugin;
    HashMap<String, int[]> unitStats = new HashMap<>();
    int[] reaperCost = {10000, 10000, 4000, 3000, 5000, 1000, 5000, 500, 500, 500, 1, 0};
    int[] lichCost = {5000, 5000, 2000, 1500, 2500, 500, 2500, 250, 250, 250, 1, 0};
    int[] eradCost = {15000, 15000, 1000, 5000, 10000, 5000, 1000, 1000, 1000, 1000, 1, 0};
    int time=0;
    static final int buildTimeIdx = 10;
    static final int unitCount = 11;
    ArrayList<Build_request> requests = new ArrayList<>();


    public UnitFactory(ExamplePlugin plugin) {
        unitStats.put(REAP, reaperCost);
        unitStats.put(LICH, lichCost);
        unitStats.put(ERAD, eradCost);
        this.plugin = plugin;
    }

    public boolean verify_request(Player player, String unitName) {
        String currentUnit = is_building();
        if (currentUnit != null) {
            int time = currentUnit_buidtime();
            player.sendMessage("Factory is currently building [orange]" + currentUnit + "[white].It will be finished in " +
                    time / 60 + "min" + time % 60 + "sec.");
            return false;
        }
        if (!unitName.equals(REAP) && !unitName.equals(LICH) && !unitName.equals(ERAD)) {
            player.sendMessage("Factory can not build [red]" + unitName + "[white]. It can build oni reaper,lich and eradicator.");
            return false;
        }
        boolean can_build = true;
        int idx = 0;
        for (Item item : content.items()) {
            if (plugin.verify_item(item)) {
                continue;
            }
            int requires = unitStats.get(unitName)[idx];
            int stored = plugin.layout[idx];
            if (requires > stored) {
                can_build = false;
                player.sendMessage("You are missing [red]" + (requires - stored) + " " + item.name + "[white].");
            }
            idx++;
        }
        if (!can_build) {
            player.sendMessage("Not enough resources!");
            return false;
        }
        return true;
    }
    public int get_unit_count(String unitName){
        if ("all".equals(unitName)) {
            return unitStats.get(LICH)[unitCount] +
                    unitStats.get(REAP)[unitCount] +
                    unitStats.get(ERAD)[unitCount];
        }
        return unitStats.get(unitName)[unitCount];
    }
    public boolean verify_deployment(Player player, String unitName){
        if(traveling){
            player.sendMessage("Units are being transported currently.They will arrive in " +
                    time / 60 + "min" + time % 60 + "sec.");
            return false;
        }
        if (!unitName.equals(REAP) && !unitName.equals(LICH) && !unitName.equals("all") && !unitName.equals(ERAD)) {
            player.sendMessage("Factory cannot build [red]" + unitName + "[white]. It can release oni reaper,lich and eradicator.");
            return false;
        }
        if (get_unit_count(unitName)==0){
            player.sendMessage("[red]There are "+get_unit_count(unitName)+" of "+unitName+" in hangar.");
            return false;
        }
        int x = (int) player.x;
        int y = (int) player.y;
        if (world.tile(x / 8, y / 8).solid()) {
            if (  unitName.equals("all") || unitName.equals(ERAD) ) {
                player.sendMessage("Land unit cant be dropped on a solid block.");
                return false;
            }
        }
        return true;

    }

    private int currentUnit_buidtime() {
        for (Build_request b : requests) {
            if (b.building) {
                return b.time;
            }
        }
        return 0;
    }

    private String is_building() {
        for (Build_request b : requests) {
            if (b.building && !b.interrupted) {
                return b.unitName;
            }
        }
        return null;
    }

    public void info(Player player) {
        boolean inProgress=false;
        for (Build_request b : requests) {
            inProgress=b.info(player);
        }
        if(!inProgress){
            player.sendMessage("Factory is not building anything nor is any unit travelling.");
        }
        if(traveling){
            player.sendMessage("Units will arrive in " +
                    time / 60 + "min" + time % 60 + "sec.");
        }else{
            player.sendMessage("No units are traveling currently.");
        }

        player.sendMessage("There are "+unitStats.get(LICH)[unitCount]+" lichs, "+unitStats.get(REAP)[unitCount]+
                " reapers and "+unitStats.get(ERAD)[unitCount]+" eradicators in hangar.");
    }

    public void interrupted() {
        interrupted = true;
    }
    public void add_units(String unitName,ArrayList<BaseUnit> units,Player player){
        BaseUnit unit = UnitTypes.lich.create(player.getTeam());
        switch (unitName){
            case REAP:
                unit = UnitTypes.reaper.create(player.getTeam());
            case ERAD:
                unit = UnitTypes.eradicator.create(player.getTeam());
        }
        for(int i=0;i<unitStats.get(unitName)[unitCount];i++){


            unit.set(player.x,player.y);
            units.add(unit);
        }
        unitStats.get(unitName)[unitCount]=0;
    }
    public void send_units(Player player,String unitName){
        traveling=true;
        Call.sendMessage("[green]"+unitName+" were launched from hangar to "+player.name+"s position.");
        ArrayList<BaseUnit> units=new ArrayList<>();
        switch (unitName) {
            case LICH:
                add_units(LICH,units,player);
                break;
            case REAP:
                add_units(REAP,units,player);
                break;
            case ERAD:
                add_units(ERAD,units,player);
                break;
            case "all":
                add_units(LICH,units,player);
                add_units(REAP,units,player);
                add_units(ERAD,units,player);
                break;
        }

        time=plugin.transport_time;
        Timer.schedule(()->{
            if(interrupted){
                Call.sendChatMessage("Units were sent back to hangar.");
                traveling=false;
                interrupted=false;
                return;
            }
            for(BaseUnit unit:units){
               unit.add();
            }
            traveling=false;
        },time);
        Timer.schedule(new TimerTask() {
            @Override
            public void run() {
                time--;
            }
        },0,1,time-1);
    }

    public void build_unit(Player player, String unitName) {
        int idx = 0;
        for (Item item : content.items()) {
            if (plugin.verify_item(item)) {
                continue;
            }
            int requires = unitStats.get(unitName)[idx];
            plugin.layout[idx] -= requires;
            idx++;
        }
        BaseUnit unit = UnitTypes.reaper.create(player.getTeam());

        switch (unitName) {
            case LICH:
                unit = UnitTypes.lich.create(player.getTeam());
                break;
            case ERAD:
                unit = UnitTypes.eradicator.create(player.getTeam());
                break;
        }
        Build_request b = new Build_request(unitName, unit,unitStats.get(unitName)[buildTimeIdx] * 60, plugin, this);
        requests.add(b);

    }
}


