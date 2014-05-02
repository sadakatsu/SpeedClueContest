using System;
using System.Net.Sockets;
using ClueSharp;

namespace ClueBot
{
  //  Derive your own Program class from ProgramTemplate<TAI>
  //  where TAI is the implementation of IClueAI which you want to use
  //  Because Main has to be static, you will have to call DoMain()
  //  from a static Main() like the following:
  //
  //  public class Program : ClueBot.ProgramTemplate<ClueStick>
  //  {
  //    public static void Main(string[] args)
  //    {
  //      new Program().DoMain(args);
  //    }
  //  }
  //
  public abstract class ProgramTemplate<T> where T : IClueAI, new() 
  {
    protected internal ClueClient m_clueClient;

    public void DoMain(string[] args)
    {
      string identifier = args[0];
      int port = Int32.Parse(args[1]);
      var client = new TcpClient("localhost", port);
      if (!client.Connected)
      {
        throw new Exception();
      }
      m_clueClient = new ClueClient(client.GetStream());
      
      m_clueClient.Handshake(identifier);

      bool end = false;
      var ai = new T();
      while (!end)
      {
        end = m_clueClient.ProcessMessage(ai);
      }

    }
  }
}
