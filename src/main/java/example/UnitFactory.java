
package example;

import arc.util.Log;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.type.Item;
import static mindustry.Vars.*;

import java.io.*;
import java.util.*;

import arc.util.Timer;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class UnitFactory {
    boolean traveling=false;
    boolean interrupted=false;

    String[] statKeys=new String[12];
    Loadout loadout;
    HashMap<String, int[]> unitStats = new HashMap<>();
    ArrayList<BuildRequest> requests = new ArrayList<>();

    int time=0;
    int buildAmount=0;
    int dropPosX =0;
    int dropPosY =0;

    private final String configFile=MyPlugin.dir+"config.json";

    static final int buildTime = 10;
    static final int buildLimit=11;
    static final int unitCount = 12;
    public static final int maxDeployment=300;
    public static final int dropPointRange=4;

    public UnitFactory(Loadout loadout) {
        int idx=0;
        for(Item item:MyPlugin.items){
            statKeys[idx]=item.name;
            idx++;
        }
        statKeys[buildTime]="buildTime";
        statKeys[buildLimit]="buildLimit";
        this.loadout=loadout;
    }

    public UnitType getUnitType(String unitName){
        for(UnitType unit:content.units()){
            if(unitName.equals(unit.name)){
                return unit;
            }
        }
        return null;
    }

    public void config(){
        try(FileReader fileReader = new FileReader(configFile)) {
            unitStats.clear();
            JSONParser jsonParser=new JSONParser();
            JSONObject settings =(JSONObject) jsonParser.parse(fileReader);
            for(Object setting:settings.keySet()){
                if(getUnitType((String)setting)==null){
                    Log.info("Unit name "+setting+" is invalid.It will be ignored.");
                    StringBuilder list=new StringBuilder();
                    for(UnitType unit:content.units()){
                        list.append(unit.name).append(" ");
                    }
                    Log.info("Valid units: "+list.toString());
                    continue;
                }
                int[] info=new int[13];
                int idx=0;
                boolean fail=false;
                JSONObject jsInfo=(JSONObject)settings.get(setting);
                for(String key:statKeys){
                    if(!jsInfo.containsKey(key)){
                        Log.info("Config loading:missing property "+key+" in "+setting+". "+setting+" will be ignored.");
                        fail=true;
                        break;
                    }
                    info[idx]=((Long)jsInfo.get(key)).intValue();
                    idx++;
                }
                if(fail){continue;}
                unitStats.put((String)setting,info);
            }
            Log.info(unitStats.size()==0 ? "Nothing to load from config file.":"Config loaded.");
        }catch (FileNotFoundException ex) {
            Log.info("No config file found.");
            createDefaultConfig();
        }catch (ParseException ex){
                Log.info("Json file is invalid");
        }catch (IOException ex){
            Log.info("Error when loading data from "+configFile+".");
        }
    }

    public void createDefaultConfig(){
        StringBuilder list=new StringBuilder();
        for(UnitType unit:content.units()){
            list.append(unit.name).append(" ");
        }
        try(FileWriter file = new FileWriter(configFile)) {

            JSONObject unit =new JSONObject();
            for(String key:statKeys){
                unit.put(key,5);
            }
            JSONObject config =new JSONObject();
            config.put("eruptor",unit);
            file.write(config.toJSONString());
            Log.info("Default "+configFile+" successfully created.Edit it and use apply-config command.");
        }catch (IOException ex){
            Log.info("Error when loading default config.");
        }
    }

    public boolean verify_request(Player player, String unitName,int amount) {
        String currentUnit = is_building();
        if (currentUnit != null) {
            int time = currentUnitBuildTime();
            player.sendMessage("[scarlet][Server][]Factory is currently building [orange]" + unitName +
                    "[white]. It will be finished in " + time / 60 + "min" + time % 60 + "sec.");
            return false;
        }
        if (!unitStats.containsKey(unitName)) {
            player.sendMessage("[scarlet][Server][]Factory cannot build [red]" + unitName +
                    "[]. It can build oni reaper,lich and eradicator.");
            return false;
        }
        if(amount==0){
            player.sendMessage("[scarlet][Server][]Done!");
            return false;
        }
        buildAmount=amount;
        boolean can_build = true;
        int idx = 0;
        for (Item item : MyPlugin.items) {
            int requires = unitStats.get(unitName)[idx]*amount;
            int stored = loadout.storage[idx];
            if (requires > stored) {
                can_build = false;
                player.sendMessage("[scarlet][Server][]You are missing [scarlet]" + (requires - stored) + " " + item.name + "[].");
            }
            idx++;
        }
        if (!can_build) {
            player.sendMessage("[scarlet][Server][]Not enough resources!");
            return false;
        }
        return true;
    }

    public int get_unit_count(String unitName){
        if ("all".equals(unitName)) {
            int res=0;
            for (String name:unitStats.keySet()){
                res+=get_unit_count(name);
            }
            return res;
        }
        return unitStats.get(unitName)[unitCount];
    }

    public boolean verify_deployment(Player player, String unitName,int amount){
        if(traveling){
            player.sendMessage("[scarlet][Server][]Units are being transported currently.They will arrive in " +
                    time / 60 + "min" + time % 60 + "sec.");
            return false;
        }
        if (!unitStats.containsKey(unitName) && !unitName.equals("all") ) {
            StringBuilder names=new StringBuilder();
            for(String name:unitStats.keySet()){
                names.append(name).append(" ");
            }
            player.sendMessage("[scarlet][Server][]Factory cannot deploy [scarlet]" + unitName + "[]. It can deploy oni:"+
                    names);
            return false;
        }
        int unitCount=get_unit_count(unitName);
        if (unitCount==0 || (unitCount<amount && !unitName.equals("all"))){
            player.sendMessage("[scarlet][Server][]There are "+get_unit_count(unitName)+" of "+unitName+" in hangar.");
            return false;
        }
        if (amount>maxDeployment){
            player.sendMessage("[scarlet][Server][]you cannot deploy mort then "+maxDeployment+" at the time.");
            return false;
        }
        int x = (int) player.x;
        int y = (int) player.y;
        if (world.tile(x / 8, y / 8).solid()) {
            if (  unitName.equals("all") || !getUnitType(unitName).flying) {
                player.sendMessage("[scarlet][Server][]Land unit cant be dropped on a solid block.");
                return false;
            }
        }
        dropPosX=x;
        dropPosY=y;
        return true;
    }

    private int currentUnitBuildTime() {
        for (BuildRequest b : requests) {
            if (b.building) {
                return b.time;
            }
        }
        return 0;
    }

    private String is_building() {
        for (BuildRequest b : requests) {
            if (b.building && !b.interrupted) {
                return b.unitName;
            }
        }
        return null;
    }

    public void interrupted() {
        interrupted = true;
    }

    public void add_units(UnitType unitType,ArrayList<BaseUnit> units,Player player,int amount){
        amount=amount==-1 ? unitStats.get(unitType.name)[unitCount]:amount;
        for(int i=0;i<amount;i++){
            BaseUnit unit=unitType.create(player.getTeam());
            unit.set(dropPosX,dropPosY);
            units.add(unit);
        }

        unitStats.get(unitType.name)[unitCount]-=amount;
    }

    public void send_units(Player player,String unitName,int amount){
        traveling=true;
        Call.sendMessage("[scarlet][Server][][green]"+unitName+" were launched from hangar to "+player.name+"s position.It will arrive in "+MyPlugin.transport_time/60+"min.");
        ArrayList<BaseUnit> units=new ArrayList<>();
        if (unitName.equals("all")){
            for (String name:unitStats.keySet()){
                add_units(getUnitType(name),units,player,-1);
            }
        }else {
            add_units(getUnitType(unitName),units,player,amount);
        }
        interrupted=false;
        time= MyPlugin.transport_time;
        Timer.schedule(()->{
            traveling=false;
            if(interrupted){
                Call.sendMessage("[scarlet][Server][]" +unitName+ " arrived to the destination, killed everything in sight but still wondering where everyone is.");
                interrupted=false;
                return;
            }
            Call.sendMessage("[green]"+unitName+" arrived");
            for(BaseUnit unit:units){
                if (!unit.isFlying()){
                    Tile tile= world.tile((int)unit.x / 8, (int)unit.y / 8);
                    if(tile.solid() && tile.breakable()) {
                        Call.sendMessage("[scarlet][Server]Ground units crashed horribly into building you built on landing point.");
                        tile.removeNet();
                        break;
                    }
                }

            }
            for(BaseUnit unit:units){
                unit.add();
            }
        },time);
        Timer.schedule(new TimerTask() {
            @Override
            public void run() {
                time--;
            }
        },0,1,time-1);
    }

    public void build_unit(String unitName,int amount) {
        int[] thisUnitstats =unitStats.get(unitName);
        for (int i=0;i<MyPlugin.items.size();i++) {
            int requires = thisUnitstats[i];
            loadout.storage[i] -= requires*amount;
        }
        int buildT=(int)(amount/(float)thisUnitstats[buildLimit]*thisUnitstats[buildTime]*60);
        BuildRequest b = new BuildRequest(unitName,buildT,buildAmount, this);
        requests.add(b);
    }

    public String info() {
        StringBuilder message=new StringBuilder();
        boolean inProgress=false;
        message.append("\nunit/in hangar\n");
        for(String name:unitStats.keySet()){
            message.append(name).append("/").append(get_unit_count(name)).append("\n");
        }
        message.append("\n");
        for (BuildRequest b : requests) {
            String info=b.info();
            if(info!=null){
                inProgress=true;
                message.append(b.info());
            }
        }
        if(!inProgress){
            message.append("Factory is not building anything.\n");
        }
        if(traveling){
            message.append("Units will arrive in ").append(time / 60).append("min").append(time % 60).append("sec.");
        }else{
            message.append("No units are traveling currently.");
        }
        return message.toString();
    }

    public String price(Player player,String unitName){
        if (!unitStats.containsKey(unitName)) {
            player.sendMessage("[scarlet][Server][]There is no [scarlet]" + unitName + "[white] only reaper,lich and eradicator.");
            return null;
        }
        StringBuilder message= new StringBuilder();
        message.append("[orange]").append(unitName.toUpperCase()).append("[white]").append("\n\n");
        message.append("in loadout / price\n");
        for(int i=0;i<10;i++){
            int inLoadout=loadout.storage[i];
            int price=unitStats.get(unitName)[i];
            if(price==0){
                continue;
            }
            message.append(price>inLoadout ? "[red]":"[white]");
            message.append(inLoadout).append(" [white]/ ").append(price).append(MyPlugin.itemIcons[i]).append("\n");
        }
        message.append("\n[red]!!![white]Factory will take resources from loadout not from the core[red]!!![white]\n");
        message.append("Build time: [orange]").append(unitStats.get(unitName)[buildTime]).append("[].\n");
        message.append("Factory can build [orange]").append(unitStats.get(unitName)[buildLimit]).append("units at the same time.");
        return message.toString();

    }

    public JSONObject get_data() {
        JSONObject data=new JSONObject();
        for (String name:unitStats.keySet()){
            data.put(name,get_unit_count(name));
        }
        return data;
    }

    public void load_data(JSONObject data){
        for (String name:unitStats.keySet()){
            unitStats.get(name)[unitCount]=((Long)data.get(name)).intValue();
        }
    }
}


