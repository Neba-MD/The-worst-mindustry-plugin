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
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;


import static mindustry.Vars.*;



public class ExamplePlugin extends Plugin{

    UnitFactory factory=new UnitFactory(this);
    Vote vote=new Vote(this,factory);
    int[] layout=new int[100];
    int layout_capacity=1000000;
    int max_transport=5000;
    int transport_time=5*60;
    int launch_amount=0;
    Item launch_item=null;
    boolean transporting=false;
    boolean launch_to_core=false;
    boolean interrupted=false;
    public ExamplePlugin(){
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
        //listen for a block selection event
        /*Events.on(BuildSelectEvent.class, event -> {
            if(!event.breaking && event.builder != null && event.builder.buildRequest() != null && event.builder.buildRequest().block == Blocks.thoriumReactor && event.builder instanceof Player){
                //send a message to everyone saying that this player has begun building a reactor
                Call.sendMessage("[scarlet]ALERT![] " + ((Player)event.builder).name + " has begun building a reactor at " + event.tile.x + ", " + event.tile.y);
            }
        });*/
        }

    private void interrupted() {
        interrupted=true;
        factory.interrupted();
        vote.interrupted();
    }


    public boolean isNotInteger(String str) {
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
    public boolean verify_item(Item item){
        return item.name.equals("blast-compound") ||
                item.name.equals("spore-pod") ||
                item.name.equals("pyratite") ||
                item.name.equals("coal") ||
                item.name.equals("sand") ||
                item.name.equals("scrap");
    }
    public Item get_item_by_name(String name){
        for(Item item:content.items()) {
            if(verify_item(item)){continue;}
            if (item.name.equals(name)) {
                return item;
            }
        }return null;
    }
    public int get_transport_amount(Item item,int amount,CoreBlock.CoreEntity core,boolean to_core){
        int idx=0;
        if(item==null){
            return 0;
        }
        for(Item _item:content.items()) {
            if(verify_item(_item)){continue;}
            if (item==_item) {
                break;
            }
            idx++;
        }
        int layout_amount=layout[idx];
        int core_amount=core.items.get(item);
        if (to_core){
            if(layout_amount<amount){
                amount=layout_amount;
            }
            if(amount>max_transport){
                amount=max_transport;
            }
        } else {

            if(amount>core_amount) {
                amount=core_amount;
            }
            if(layout_amount+amount>layout_capacity){
                amount=layout_capacity-layout_amount;
            }
        }
        return amount;
    }
    /*public int transport_item(Item item,int amount,CoreBlock.CoreEntity core,boolean inport){
        int idx=0;
        for(Item _item:content.items()) {
            if(verifi_item(_item)){continue;}
            if (item==_item) {
                break;
            }
            idx++;
        }
        int layout_amount=layout[idx];
        int core_amount=core.items.get(item);
        if (inport){
            if(layout_amount<amount){
                amount=layout_amount;
            }
            layout[idx]-=amount;
            core.items.add(item,amount);
        } else {
            if(amount>core_amount) {
                amount=core_amount;
            }
            if(layout_amount+amount>layout_capacity){
                amount=layout_capacity-layout_amount;
            }

            core.items.remove(item,amount);
            layout[idx]+=amount;
        }
        return amount;

    }*/
    public boolean set_transport_inf(String sItem,String sAmount,Player player,boolean can_all){
        if(vote.isIsvoting()){return true;}
        if(isNotInteger(sAmount)){
            player.sendMessage("[scarlet]You entered wrong amount!");
            return false;
        }
        launch_amount=Integer.parseInt(sAmount);
        if(sItem.equals("all") && can_all){
            launch_item=null;
            return true;
        }
        Item picked_item=get_item_by_name(sItem);
        if (picked_item==null){
            StringBuilder message= new StringBuilder("  ");
            for(Item item:content.items()) {
                if (verify_item(item)) {
                    continue;
                }
                message.append(item.name).append("  ");
            }
            player.sendMessage("[scarlet]You taped the name of item wrong!");
            player.sendMessage("List of items:"+message.toString());
            return false;
        }
        launch_item=picked_item;
        return true;
    }
    private void show_layout(Player player) {
        int idx=0;
        StringBuilder message= new StringBuilder();
        for(Item item:content.items()){
            if(verify_item(item)){continue;}
            message.append(layout[idx] != layout_capacity ? "[white]" : "[green]");
            message.append(item.name).append(":").append(layout[idx]).append("  ");
            idx++;
        }
        player.sendMessage(message.toString());
    }
    public void use_layout(Player player){
        Teams.TeamData teamData = state.teams.get(player.getTeam());
        CoreBlock.CoreEntity core = teamData.cores.first();
        int idx=0;
        for(Item _item:content.items()) {
            if(verify_item(_item)){continue;}
            if (launch_item==_item) {
                break;
            }
            idx++;
        }
        int amount=get_transport_amount(launch_item,launch_amount,core,launch_to_core);
        String message=(launch_item==null ? "all" : amount +" "+launch_item.name);
        if(launch_to_core){
            layout[idx]-=amount;
            transporting=true;
            int finalIdx = idx;
            Timer.schedule(()->{
                transporting=false;
                if(interrupted){
                    Call.sendMessage("Base is gone ,[orange]"+message+"[white] going back to loadout.");
                    layout[finalIdx]+=amount;
                    return;
                }
                core.items.add(launch_item,amount);
                Call.sendMessage(message+" arrived to core");
            },transport_time);
        }else{
            if(launch_item==null){
                int index=0;
                for(Item item:content.items()) {
                    if (verify_item(item)) {
                        continue;
                    }
                    int finalAmount=get_transport_amount(item,launch_amount,core,launch_to_core);
                    core.items.remove(item,finalAmount);
                    layout[index] += finalAmount;
                    index++;
                }
            }else {
                core.items.remove(launch_item, amount);
                layout[idx] += amount;
            }
        }

    }

    /*public void use_layout(Player player,String type,String sAmount,boolean inport ){
        boolean all=type.equals("all");
        if (all && !inport){
        }else if(!isInteger(sAmount)){
            player.sendMessage("[scarlet]You entered wrong amount!");
            return;
        }
        Teams.TeamData teamData = state.teams.get(player.getTeam());
        CoreBlock.CoreEntity core = teamData.cores.first();
        int amount=Integer.parseInt(sAmount);
        Item picked_item=get_item_by_name(type);
        if (picked_item==null && !all){
            StringBuilder messange= new StringBuilder("  ");
            for(Item item:content.items()) {
                if (verifi_item(item)) {
                    continue;
                }
                messange.append(item.name).append("  ");
            }
            player.sendMessage("[scarlet]You taped the name of item wrong!");
            player.sendMessage("List of items:"+messange.toString());
            return;
        }
        if(inport && amount>max_transport){
            amount=max_transport;
            transporting=true;
        }
        int finalAmount = amount;
        Timer.schedule(()->{
            if(all){
                for(Item item:content.items()) {
                    if (verifi_item(item)) {
                        continue;
                    }
                    transport_item(item, finalAmount, core, inport);
                }
            }else {
                transport_item(picked_item, finalAmount, core, inport);
            }
            transporting=false;
            if(inport){
                Call.sendMessage(type+" arrived to core");
            }
        },inport ? transport_time:0);
        Call.sendMessage(type+" wos launched to "+ (inport ? "core":"layout"));
    }*/
    public int get_storage_size() {
        int res=0;
        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                //loop through and log all found reactors
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
        /*handler.register("reactors", "List all thorium reactors in the map.", args -> {
            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    //loop through and log all found reactors
                    if(world.tile(x, y).block() == Blocks.thoriumReactor){
                        Log.info("Reactor at {0}, {1}", x, y);
                    }
                }
            }
        });*/
        handler.register("test","test",args->{
            Log.info("start");
            Log.info("finish");
                });
        handler.register("set-transport_time","<seconds>","Sets the ladout-usees cooldown.",args->
        {
            if(isNotInteger(args[0])){
                Log.info("You have to write an integer Neba!");
                return;
            }
            transport_time=Integer.parseInt(args[0]);
            Log.info("Transport time wos set.");

        });
        handler.register("set-max_transport","<seconds>","Sets the laudout-usees maximal transport .",args->
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
        handler.<Player>register("loadout-help","Shows better explanation of loadout system.",
                (arg,player)-> player.sendMessage("Loadout is storage in your home base.You can launch resources " +
                        "and save them for later use.When using resources from Loadout it takes some time for spaceships " +
                        "to arrive with resource,but you can always launch.Other players have to agree with loadout use."));

        handler.<Player>register("loadout-show","Shows how may resource you have stored in the layout.",(arg, player) -> {
            player.sendMessage("[green]LAYOUT STATE");
            show_layout(player);
        });

        handler.<Player>register("loadout-use","<item> <amount>","Uses layout resources up to [orange]"+
                max_transport+"[white].",(arg, player) -> {
            if(transporting){
                player.sendMessage("[orange]Resources are currently being transported[white] by our spaceships.You have to wait for them to arrive!");
                return;
            }
            if (set_transport_inf(arg[0], arg[1],player,false)){
                launch_to_core=true;
                vote.launch_Vote(player,"use");
            }
        });

        handler.<Player>register("loadout-fill","<item/all> <amount>","Fills layout with resources " +
                "from core up to [orange]"+layout_capacity+" [white]for each resource",(arg, player) -> {
            if (set_transport_inf(arg[0],arg[1],player,true)){
                launch_to_core=false;
                vote.launch_Vote(player,"fill");
            }
        });

        handler.<Player>register("factory-build-unit","<unitName>","Sends build request to factory that will then build " +
                "unit from loadout resources and send it to us.",(arg, player) -> {
            if(!factory.verify_request(player,arg[0])) {
                return;
            }
            vote.build_Vote(player,"build",arg[0]);
        });

        handler.<Player>register("factory-progress","Displays traveling and building progress of units."
                , (arg, player) -> {
            factory.info(player);
        });
        handler.<Player>register("factory-release","<unit/all>","Sends all units or only specified type " +
                "to your position.",(arg, player) -> {
            if (factory.verify_deployment(player,arg[0])){
                vote.build_Vote(player,"release",arg[0]);
            }
        });
    }
}

