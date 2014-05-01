using System;
using System.Net.Sockets;

namespace ClueBot
{
  class Program
  {
    private static ClueClient _clueClient;

    static void Main(string[] args)
    {
      string identifier = args[0];
      int port = Int32.Parse(args[1]);
      var client = new TcpClient("localhost", port);
      if (!client.Connected)
      {
        throw new Exception();
      }
      _clueClient = new ClueClient(client.GetStream());
      
      _clueClient.Handshake(identifier);

      bool end = false;
      var ai = new ClueStick.ClueStick();
      while (!end)
      {
        end = _clueClient.ProcessMessage(ai);
      }

    }
  }
}
