package example;

import arc.util.Timer;
import mindustry.entities.type.Player;
import mindustry.game.Teams;
import mindustry.gen.Call;
import mindustry.type.Item;
import mindustry.world.blocks.storage.CoreBlock;
import org.json.simple.JSONObject;

import java.util.TimerTask;

import static mindustry.Vars.state;

public class Loadout{
    Item launch_item;
    
    int[] storage =new int[10];
    int capacity =1000000;
    int launch_amount=0;
    int time;
    
    boolean launch_to_core=false;
    boolean transporting=false;
    boolean interrupted=false;
    
    public Item get_item_by_name(String name){
        for(Item item:MyPlugin.items) {
            if (item.name.equals(name)) {
                return item;
            }
        }return null;
    }

    public boolean set_transport_inf(String sItem,String sAmount,Player player,boolean can_all,boolean to_core){
        launch_to_core=to_core;
        if(transporting){
            player.sendMessage("[scarlet][Server][][orange]"+launch_amount+" "+launch_item.name+"[white] is currently being transported." +
                    "you have to wait " + time / 60 + "min" + time % 60 + "sec for it to arrive.");
            return false;
        }
        if(MyPlugin.isNotInteger(sAmount)){
            player.sendMessage("[scarlet][Server][]You entered wrong amount!");
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
            for(Item item:MyPlugin.items) {
                message.append(item.name).append("  ");
            }
            player.sendMessage("[scarlet][Server][]You taped the name of item wrong!");
            player.sendMessage("List of items:"+message.toString());
            return false;
        }
        launch_item=picked_item;

        Teams.TeamData teamData = state.teams.get(player.getTeam());
        CoreBlock.CoreEntity core = teamData.cores.first();
        if(get_transport_amount(launch_item,launch_amount,core,launch_to_core)==0){
            player.sendMessage("[scarlet]Nothing to transport!");
            return false;
        }
        return true;
    }

    public int get_transport_amount(Item item,int amount,CoreBlock.CoreEntity core,boolean to_core){
        if(item==null){
            return -1;
        }
        int idx=getItemIndex(item);
        int loadout_amount=storage[idx];
        int core_amount=core.items.get(item);
        if (to_core){
            if(loadout_amount<amount){
                amount=loadout_amount;
            }
            if(amount>MyPlugin.max_transport){
                amount=MyPlugin.max_transport;
            }
        } else {

            if(amount>core_amount) {
                amount=core_amount;
            }
            if(loadout_amount+amount>capacity){
                amount=capacity-loadout_amount;
            }
        }
        return amount;
    }

    public int getItemIndex(Item item){
        return MyPlugin.items.indexOf(item);
    }

    public void use_loadout(Player player){
        Teams.TeamData teamData = state.teams.get(player.getTeam());
        CoreBlock.CoreEntity core = teamData.cores.first();
        int idx=getItemIndex(launch_item);
        int amount=get_transport_amount(launch_item,launch_amount,core,launch_to_core);
        String message=(launch_item==null ? "all" : amount +" "+launch_item.name);

        if(launch_to_core){
            storage[idx]-=amount;
            transporting=true;
            interrupted=false;
            time=MyPlugin.transport_time;
            launch_amount=amount;
            Timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    time--;
                }
            },0,1,time-1);
            Item finalItem=launch_item;
            Timer.Task transport=new Timer.Task() {
                @Override
                public void run() {
                    transporting=false;
                    if(interrupted){
                        Call.sendMessage("[scarlet][Server][]Base is gone ,[orange]"+message+"[white] going back to loadout.");
                        storage[idx]+=amount;
                        interrupted=false;
                        return;
                    }
                    core.items.add(finalItem,amount);
                    Call.sendMessage("[scarlet][Server][green]"+message+" arrived to core");
                }
            };
            Timer.schedule(transport,time);
        }else{
            if(launch_item==null){
                int index=0;
                for(Item item:MyPlugin.items) {
                    int finalAmount=get_transport_amount(item,launch_amount,core,launch_to_core);
                    core.items.remove(item,finalAmount);
                    storage[index] += finalAmount;
                    index++;
                }
            }else {
                core.items.remove(launch_item, amount);
                storage[idx] += amount;
            }
            Call.sendMessage("[scarlet][Server][green]"+message+" arrived to loadout");
        }
    }

    public String info(){
        String shipReport;
        if( transporting){
            shipReport="[orange]"+(launch_item==null ? "all":(launch_amount+" "+launch_item.name))+"[white] will arrive in " +
                    time / 60 + "min" + time % 60 + "sec.";
        }else{
            shipReport="Ship is ready for transport of resources.";
        }
        int idx=0;
        StringBuilder message= new StringBuilder();
        message.append("\n");
        for(Item item:MyPlugin.items){
            message.append(storage[idx] != capacity ? "[white]" : "[green]");
            message.append(storage[idx]).append(MyPlugin.itemIcons[idx]).append("\n");
            idx++;
        }
        message.append("[white]\n");
        message.append(shipReport);
        return message.toString();
    }

    public void interrupted() {
        interrupted=true;
    }

    public JSONObject get_data() {
        JSONObject data=new JSONObject();
        int idx=0;
        for(Item item:MyPlugin.items){
            data.put(item.name,storage[idx]);
            idx++;
        }
        return data;
    }

    public void load_data(JSONObject data){
        int idx=0;
        for(Item item:MyPlugin.items){
            storage[idx]=((Long)data.get(item.name)).intValue();
            idx++;
        }
    }
}
