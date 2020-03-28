package example;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Timer;
import mindustry.content.Blocks;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.game.Teams;
import mindustry.gen.Call;
import mindustry.plugin.Plugin;
import mindustry.type.Item;
import mindustry.type.ItemType;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;

import java.io.*;
import java.util.ArrayList;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static mindustry.Vars.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MyPlugin extends Plugin{
    Loadout loadout;
    UnitFactory factory;
    Vote vote;

    String[] saveReq={"loadout","factory"};
    static String dir="config/mods/myPlugin/";
    static String[] itemIcons={"\uF838","\uF837","\uF836","\uF835","\uF832","\uF831","\uF82F","\uF82E","\uF82D","\uF82C"};
    static int max_transport=5000;
    static int transport_time=5*60;
    static ArrayList<Item> items=new ArrayList<>();

    int autoSaveFrequency=5;

    private final String filename=dir+"data.json";



    public MyPlugin(){

        Events.on(EventType.PlayerChatEvent.class, e -> {
            String check = String.valueOf(e.message.charAt(0));
            if (!check.equals("/") && vote.isIsvoting()) {
                if (e.message.equals("y")) {
                    vote.add_vote(e.player, 1);
                }
            }
        });

        Events.on(EventType.BuildSelectEvent.class, e->{
            double dist=sqrt((pow(e.tile.x-(float)(factory.dropPosX/8),2)+pow(e.tile.y-(float)(factory.dropPosY/8),2)));
            if(factory.traveling &&  dist<UnitFactory.dropPointRange) {
                e.tile.removeNet();
                ((Player)e.builder).sendMessage("[scarlet][Server]You cannot build on unit drop point.");
            }

        });

        Events.on(EventType.WorldLoadEvent.class,e-> interrupted());

        Events.on(EventType.ServerLoadEvent.class,e->{
            loadItems();
            loadout=new Loadout();
            factory=new UnitFactory(loadout);
            vote=new Vote(factory,loadout);
            if(!makeDir()){
                Log.info("There wos a problem with creating "+dir+" please add it manually.");
                return;
            }
            //load_data();
            Log.info("Saves once a "+autoSaveFrequency+"min.");
            autoSave();
            load_data();
            factory.config();



        });
        /*Events.on(EventType.BuildSelectEvent.class, event -> {
            if(!event.breaking && event.builder != null && event.builder.buildRequest() != null && event.builder.buildRequest().block == Blocks.thoriumReactor && event.builder instanceof Player){
                //send a message to everyone saying that this player has begun building a reactor
                Call.sendMessage("[scarlet]ALERT![] " + ((Player)event.builder).name + " has begun building a reactor at " + event.tile.x + ", " + event.tile.y);
            }
        });*/
        }

    private void loadItems(){
        for(Item item:content.items()){
            if(item.type!= ItemType.material){continue;}
            items.add(item);
        }
    }

    private boolean makeDir(){
        File dir=new File(MyPlugin.dir);
        if(!dir.exists()){
            return dir.mkdir();
        }
        return true;
    }

    private void interrupted() {
        factory.interrupted();
        if(vote.isIsvoting()) {
            vote.cancel();
        }
        loadout.interrupted();
    }

    private void load_data() {

        try(FileReader fileReader = new FileReader(filename)) {

            JSONParser jsonParser=new JSONParser();
            Object obj=jsonParser.parse(fileReader);
            JSONObject saveData=(JSONObject)obj;
            for(String r:saveReq){
                if(!saveData.containsKey(r)){
                    Log.info("Failed to load save file.");
                    return;
                }
            }
            loadout.load_data((JSONObject)saveData.get("loadout"));
            factory.load_data((JSONObject)saveData.get("factory"));
            fileReader.close();
            Log.info("Data loaded.");
        }catch (FileNotFoundException ex) {
            Log.info("No saves found.New save file " + filename + " will be created.");
            save_data();
        }catch (ParseException ex){
            Log.info("Json file is invalid.");
        }catch (IOException ex){
            Log.info("Error when loading data from "+filename+".");
        }

    }

    private void save_data(){

        JSONObject savedata=new JSONObject();
        savedata.put("loadout",loadout.get_data());
        savedata.put("factory",factory.get_data());
        try(FileWriter file = new FileWriter(filename))
        {
            file.write(savedata.toJSONString());
            file.close();
            Log.info("Data saved.");
        }catch (IOException ex){
            Log.info("Error when saving data.");
        }

    }

    private void autoSave(){
        Timer.schedule(()->{
            save_data();
            autoSave();
        },autoSaveFrequency*60);
    }

    public static boolean isNotInteger(String str) {
        if(str == null || str.trim().isEmpty()) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if(!Character.isDigit(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }
    
    public int getStorageSize() {
        int res=0;
        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                Block block = world.tile(x, y).block();
                if (Blocks.coreShard.equals(block)) {

                    res += 4000;
                } else if (Blocks.coreFoundation.equals(block)) {
                    res += 9000;
                } else if (Blocks.coreNucleus.equals(block)) {
                    res += 13000;
                }
            }
        }
        return res;
    }

    private int getAmount(String[] arg,Player player){
        if(arg.length==1){
            return 1;
        }
        if(isNotInteger(arg[1])){
            player.sendMessage("[scarlet][Server][]Amount has to be integer.");
            return -1;
        }
        return Integer.parseInt(arg[1]);
    }

    public void build_core(int cost,Player player,Block core_tipe){
        boolean can_build=true;
        Teams.TeamData teamData = state.teams.get(player.getTeam());
        CoreBlock.CoreEntity core = teamData.cores.first();
        for(Item item:items){
            if (!core.items.has(item, cost)) {
                can_build=false;
                player.sendMessage("[scarlet]" + item.name + ":" + core.items.get(item) +"/"+ cost);
            }
        }
        if(can_build) {
            Call.onConstructFinish(world.tile(player.tileX(), player.tileY()), core_tipe, 0, (byte) 0, player.getTeam(), false);
            if (world.tile(player.tileX(), player.tileY()).block() == core_tipe) {
                player.sendMessage("[green]Core spawned!");
                Call.sendMessage("[scarlet][Server][]Player [green]"+player.name+" []has taken a portion of resources to build a core!");
                for(Item item:items){
                    core.items.remove(item, cost);
                }
            } else {
                player.sendMessage("[scarlet][Server]Core spawn failed!Invalid placement!");
            }
            return;
        }
        player.sendMessage("[scarlet][Server]Core spawn failed!Not enough resorces.");
    }
    
    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("myplugin-save","saves loadout and factory progress immediately.",args->
                save_data());
        handler.register("myplugin-load","loads save data if there is any.",args->
                load_data());
        handler.register("set-save-freq","<minutes>","sets autosave s frequency.",args->{
            if(isNotInteger(args[0])){
                Log.info("You have to write an integer Neba!");
                return;
            }
            autoSaveFrequency=Integer.parseInt(args[0]);
            Log.info("Autosave frequency wos set.");
        });
        handler.register("apply-config","Loads data from config file.",args-> factory.config());

        handler.register("set-trans-time","<seconds>","Sets the loadout-use cool down.",args->
        {
            if(isNotInteger(args[0])){
                Log.info("You have to write an integer Neba!");
                return;
            }
            transport_time=Integer.parseInt(args[0]);
            Log.info("Transport time wos set.");

        });
        handler.register("set-max-trans","<seconds>","Sets the laudout-usees maximal transport .",args->
        {
            if(isNotInteger(args[0])){
                Log.info("You have to write an integer Neba!");
                return;
            }
            max_transport=Integer.parseInt(args[0]);
            Log.info("Max transport wos set.");

        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){

        /*handler.<Player>register("add","add items",(args,player)->{
            Teams.TeamData teamData = state.teams.get(player.getTeam());
            CoreBlock.CoreEntity core = teamData.cores.first();
            for(Item item:items){
                core.items.add(item, 40000);
            }
        });*/

        handler.<Player>register("build-core","<small/normal/big>", "Makes new core", (arg, player) -> {
            // Core type
            int storage= getStorageSize();
            Block to_build = Blocks.coreShard;
            int cost=(int)(storage*.25f);
            switch(arg[0]){
                case "normal":
                    to_build = Blocks.coreFoundation;

                    cost=(int)(storage*.5f);
                    break;
                case "big":
                    to_build = Blocks.coreNucleus;
                    cost=(int)(storage*.75f);
                    break;
            }
            build_core(cost,player,to_build);
        });

        handler.<Player>register("l-help","Shows better explanation of loadout system.",
                (arg,player)-> player.sendMessage("l=Loadout is storage in your home base. You can launch resources " +
                        "and save them for later use. When using resources from Loadout it takes some time for spaceships " +
                        "to arrive with resource,but you can always launch. Other players have to agree with loadout use."));

        handler.<Player>register("l-info","Shows how may resource you have stored in the loadout."
                ,(arg, player) -> {
            String message="[orange]LOADOUT INFO\n";
            message+=loadout.info();
            Call.onInfoMessage(player.con,message);
        });

        handler.<Player>register("l-use","<item> <amount>","Uses loadout resources up to [orange]"+
               max_transport+"[white].",(arg, player) -> {
            if (vote.check(player) && loadout.set_transport_inf(arg[0], arg[1],player,false,true)){
                vote.loadout_Vote(player,"use");
            }
        });

        handler.<Player>register("l-fill","<item/all> <amount>","Fills loadout with resources " +
                "from core up to [orange]"+loadout.capacity+" [white]for each resource",(arg, player) -> {
            if (vote.check(player) && loadout.set_transport_inf(arg[0],arg[1],player,true,false)){
                vote.loadout_Vote(player,"fill");
            }
        });
        
        handler.<Player>register("f-help","Shows better explanation of factory system.",
                (arg,player)-> player.sendMessage(
                        "f=Factory is on our home base. Its capable of building advanced units,storing them in " +
                "hangar or sending then to your position. It can build lich,reaper and eradicator for a reasonable " +
                "amount of resources. Be aware of that factory can use only resources in loadout."));

        handler.<Player>register("f-info","Displays traveling and building progress of units."
                , (arg, player) ->{
                    String message="[orange]FACTORY INFO[]\n";
                    message+=factory.info();
                    Call.onInfoMessage(player.con,message);
        });

        handler.<Player>register("f-build","<unitName> [amount]","Sends build request to factory that will then build " +
                "unit from loadout resources and send it to us.",(arg, player) -> {
            int amount=getAmount(arg,player);
            if(amount==-1){
                return;
            }
            if(vote.check(player) && factory.verify_request(player,arg[0],amount)) {
                vote.factory_Vote(player,"build",arg[0],amount);
            }
        });



        handler.<Player>register("f-release","<unit/all> [amount]","Sends all units or only an amount of specified type " +
                "to your position.",(arg, player) -> {
            int amount=getAmount(arg,player);
            if(amount==-1){
                return;
            }
            if (vote.check(player) && factory.verify_deployment(player,arg[0],amount)){
                vote.factory_Vote(player,"release",arg[0],amount);
            }
        });
        handler.<Player>register("f-price-of" ,"<unit-name>","Displays pricing of units."
                ,(arg, player) ->{
            String message=factory.price(player,arg[0]);
            if (message==null){return;}
            Call.onInfoMessage(player.con,message);
                });
    }
   
    
}
