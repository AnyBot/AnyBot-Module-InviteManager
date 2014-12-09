/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.anynet.anybot.module.invitemanager;

import eu.anynet.anybot.bot.Bot;
import eu.anynet.anybot.bot.Module;
import eu.anynet.anybot.pircbotxextensions.MessageEventEx;
import eu.anynet.java.util.TimerTask;
import java.util.HashMap;
import org.pircbotx.hooks.events.InviteEvent;

/**
 *
 * @author sim
 */
public class InviteManager extends Module
{

   private HashMap<String,TimerTask> joinTimer;

   public InviteManager()
   {
      this.joinTimer = new HashMap<>();
   }

   @Override
   public void launch()
   {

   }

   @Override
   public void onMessage(MessageEventEx event) throws Exception
   {
      boolean isop = event.getChannel().isOp(event.getUser()) ||
              event.getChannel().isSuperOp(event.getUser()) ||
              event.getChannel().isOwner(event.getUser());

      if(event.args().isBotAsked() && event.isChannelMessage() && event.args().count()>0)
      {
         String cmd = event.args().get(0);

         if(cmd.equalsIgnoreCase("confirm"))
         {
            String channame = event.getChannel().getName();
            if(this.joinTimer.containsKey(channame))
            {
               TimerTask tsk = this.joinTimer.get(channame);
               if(isop)
               {
                  event.respond("Thank you.");
                  tsk.stop();
                  this.joinTimer.remove(channame);
               }
               else
               {
                  event.respond("OP required.");
               }
            }
         }
         else if(cmd.equalsIgnoreCase("part"))
         {
            String channame = event.getChannel().getName();
            if(isop)
            {
               event.respond("Bye!");
               event.getBot().sendRaw().rawLine("PART "+channame);
               if(!this.joinTimer.containsKey(channame))
               {
                  this.joinTimer.get(channame).stop();
                  this.joinTimer.remove(channame);
               }
            }
            else
            {
               event.respond("OP required.");
            }
         }

      }
   }

   @Override
   public void onInvite(InviteEvent<Bot> event) throws Exception
   {
      final InviteManager me = this;
      //this.writePipeLine("Invite request for "+event.getChannel()+" by "+event.getUser());
      event.getBot().sendIRC().joinChannel(event.getChannel());

      event.getBot().sendIRC().message(event.getChannel(), "Hello. "+event.getUser()+" invited me to this channel. "+
              "Please confirm this with the following command within 60 secounds: \""+event.getBot().getNick()+": confirm\" (OP required)");

      final InviteEvent<Bot> ev = event;
      if(!me.joinTimer.containsKey(ev.getChannel()))
      {
         TimerTask tsk = new TimerTask(60000, false, true)
         {
            @Override
            public void doWork()
            {
               String chan = ev.getChannel();
               me.joinTimer.remove(chan);
               ev.getBot().sendIRC().message(chan, "Time is over. Bye.");
               ev.getBot().sendRaw().rawLine("PART "+chan);
            }
         };
         me.joinTimer.put(ev.getChannel(), tsk);
         tsk.start();
      }

   }

}
