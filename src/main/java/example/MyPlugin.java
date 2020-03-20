package example;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
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

import static mindustry.Vars.*;

public class MyPlugin extends Plugin{

    Loadout loadout=new Loadout();
    UnitFactory factory=new UnitFactory(loadout);
    Vote vote=new Vote(factory,loadout);
    static String[] itemIcons={"\uF838","\uF837","\uF836","\uF835","\uF832","\uF831","\uF82F","\uF82E","\uF82D","\uF82C"};

    int loadout_capacity=1000000;
    static int max_transport=5000;
    static int transport_time=5*60;
    

    public MyPlugin(){
        Events.on(EventType.PlayerChatEvent.class, e -> {
            String check = String.valueOf(e.message.charAt(0));
            if (!check.equals("/") && vote.isIsvoting()) {
                if (e.message.equals("y")) {
                    vote.add_vote(e.player, 1);
                } else if (e.message.equals("n")) {
                    vote.add_vote(e.player, 1);
                }
            }
        });
        Events.on(EventType.GameOverEvent.class,e-> interrupted());
        /*Events.on(BuildSelectEvent.class, event -> {
            if(!event.breaking && event.builder != null && event.builder.buildRequest() != null && event.builder.buildRequest().block == Blocks.thoriumReactor && event.builder instanceof Player){
                //send a message to everyone saying that this player has begun building a reactor
                Call.sendMessage("[scarlet]ALERT![] " + ((Player)event.builder).name + " has begun building a reactor at " + event.tile.x + ", " + event.tile.y);
            }
        });*/
        }
    private void interrupted() {
        factory.interrupted();
        vote.interrupted();
        loadout.interrupted();
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
    public static boolean verify_item(Item item){
        return item.type!= ItemType.material;
    }
    
    public int get_storage_size() {
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
    public void build_core(int cost,Player player,Block core_tipe){
        boolean can_build=true;
        Teams.TeamData teamData = state.teams.get(player.getTeam());
        CoreBlock.CoreEntity core = teamData.cores.first();
        for(Item item:content.items()){
            if(verify_item(item)){continue;}
            if (!core.items.has(item, cost)) {
                can_build=false;
                player.sendMessage("[scarlet]" + item.name + ":" + core.items.get(item) +"/"+ cost);
            }
        }
        if(can_build) {
            Call.onConstructFinish(world.tile(player.tileX(), player.tileY()), core_tipe, 0, (byte) 0, player.getTeam(), false);
            if (world.tile(player.tileX(), player.tileY()).block() == core_tipe) {

                player.sendMessage("[green]Core spawned!");
                for(Item item:content.items()){
                    if(verify_item(item)){continue;}
                    core.items.remove(item, cost);
                }

            } else {
                player.sendMessage("[scarlet]Core spawn failed!Invalid placement!");
            }
            return;
        }

        player.sendMessage("[scarlet]Core spawn failed!Not enough resorces.");
    }
    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("test","test",args->{
            Log.info("start");
            Log.info("finish");
                });
        handler.register("set-trans-time","<seconds>","Sets the ladout-usees cooldown.",args->
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
            Log.info("Transport time wos set.");

        });
    }
    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){

        handler.<Player>register("add","add items",(args,player)->{
            Teams.TeamData teamData = state.teams.get(player.getTeam());
            CoreBlock.CoreEntity core = teamData.cores.first();
            for(Item item:content.items()){

                if(verify_item(item)){continue;}
                core.items.add(item, 40000);
            }
        });
        handler.<Player>register("build-core","<small/normal/big>", "Makes new core", (arg, player) -> {
            // Core type
            int storage= get_storage_size();
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
                (arg,player)-> player.sendMessage("l=Loadout is storage in your home base.You can launch resources " +
                        "and save them for later use.When using resources from Loadout it takes some time for spaceships " +
                        "to arrive with resource,but you can always launch.Other players have to agree with loadout use."));

        handler.<Player>register("l-info","Shows how may resource you have stored in the loadout."
                ,(arg, player) -> {
            String message="[orange]LOADOUT INFO\n";
            message+=loadout.info();
            Call.onInfoMessage(player.con,message);
        });

        handler.<Player>register("l-use","<item> <amount>","Uses loadout resources up to [orange]"+
                max_transport+"[white].",(arg, player) -> {
            if (loadout.set_transport_inf(arg[0], arg[1],player,false,true)){
                vote.loadout_Vote(player,"use");
            }
        });

        handler.<Player>register("l-fill","<item/all> <amount>","Fills loadout with resources " +
                "from core up to [orange]"+loadout_capacity+" [white]for each resource",(arg, player) -> {
            if (loadout.set_transport_inf(arg[0],arg[1],player,true,false)){
                vote.loadout_Vote(player,"fill");
            }
        });
        handler.<Player>register("f-help","Shows better explanation of factory system.",(arg,player)->{
            player.sendMessage("f=Factory is on our home base.Its capable of building advanced units,storing them in " +
                    "hangar or sending then to your position.It can build lich,reaper and eradicator for a reasonable " +
                    "amount of resources.Be aware of that factory can use only resources in loadout.");
                });

        handler.<Player>register("f-build","<unitName>","Sends build request to factory that will then build " +
                "unit from loadout resources and send it to us.",(arg, player) -> {
            if(!factory.verify_request(player,arg[0])) {
                return;
            }
            vote.factory_Vote(player,"build",arg[0]);
        });

        handler.<Player>register("f-info","Displays traveling and building progress of units."
                , (arg, player) ->{
            player.sendMessage("[orange]FACTORY INFO");
            factory.info(player);
                });

        handler.<Player>register("f-release","<unit/all>","Sends all units or only specified type " +
                "to your position.",(arg, player) -> {
            if (factory.verify_deployment(player,arg[0])){
                vote.factory_Vote(player,"release",arg[0]);
            }
        });
        handler.<Player>register("f-price-of" ,"<unit-name>","Displays priceing of units."
                ,(arg, player) ->{
            String message=factory.price(player,arg[0]);
            if (message==null){return;}
            Call.onInfoMessage(player.con,message);
                });
    }
}
