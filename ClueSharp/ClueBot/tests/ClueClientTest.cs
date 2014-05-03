using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using ClueSharp;
using NUnit.Framework;

namespace ClueBot.tests
{
  [TestFixture]
  class ClueClientTest
  {
    private DummyClient client;
    private DummyAI ai;

    class DummyAI : IClueAI
    {
      private readonly List<string> m_records;

      public DummyAI()
      {
        m_records = new List<string>();
      }

      public List<string> Records
      {
        get { return m_records; }
      }

      public void Reset(int n, int i, IEnumerable<Suspect> suspects, IEnumerable<Weapon> weapons, IEnumerable<Room> rooms)
      {
        Records.Add("reset");
      }

      public void Suggestion(int suggester, MurderSet suggestion, int? disprover, Card disproof)
      {
        Records.Add("suggestion");
      }

      public void Accusation(int accuser, MurderSet suggestion, bool won)
      {
        Records.Add("accusation");
      }

      public MurderSet Suggest()
      {
        Records.Add("suggest");
        return new MurderSet();
      }

      public MurderSet? Accuse()
      {
        Records.Add("accuse");
        return null;
      }

      public Card Disprove(int player, MurderSet suggestion)
      {
        Records.Add("disprove");
        return null;
      }
    }

    class DummyClient : ClueClient
    {
      private readonly List<string> m_sent;

      public DummyClient()
        : base(null)
      {
        m_sent = new List<string>();
      }

      public List<string> Sent
      {
        get { return m_sent; }
      }

      protected override void Send(string msg)
      {
        Sent.Add(msg);
      }
    }

    [SetUp]
    public void Init()
    {
      ai = new DummyAI();
      client = new DummyClient();
    }

    [Test]
    public void TestProcessMessageString()
    {
      var s = "reset 4 3 Gr Sc St Bi";
      client.ProcessMessageString(ai, s);
      Assert.AreEqual("reset", ai.Records[0]);
      Assert.AreEqual("ok", client.Sent[0]);
    }

    [Test]
    public void TestProcessMessageStringSuggest()
    {
      var s = "suggest";
      client.ProcessMessageString(ai, s);
      Assert.AreEqual("suggest", ai.Records[0]);
      Assert.AreEqual(1, client.Sent.Count);
      var suggestResponseRegex = new Regex("suggest( [A-Z][a-z]){3}");
      Assert.IsTrue(suggestResponseRegex.IsMatch(client.Sent[0]));
    }

    [Test]
    public void TestProcessMessageStringDisprove()
    {
      var s = "disprove 0 Sc Ca Bi";
      client.ProcessMessageString(ai, s);
      Assert.AreEqual("disprove", ai.Records[0]);
      Assert.AreEqual(1, client.Sent.Count);
      var suggestResponseRegex = new Regex("-|show( [A-Z][a-z]){0,1}");
      Assert.IsTrue(suggestResponseRegex.IsMatch(client.Sent[0]));
    }


    [Test]
    public void TestProcessMessageStringAccuse()
    {
      var s = "accuse";
      client.ProcessMessageString(ai, s);
      Assert.AreEqual("accuse", ai.Records[0]);
      Assert.AreEqual(1, client.Sent.Count);
      var suggestResponseRegex = new Regex("-|accuse( [A-Z][a-z]){3}");
      Assert.IsTrue(suggestResponseRegex.IsMatch(client.Sent[0]));
    }

    [Test]
    public void TestProcessMessageStringWithNulls()
    {
      var s = "reset 4 3 Gr Sc St Bi\0\0\0\0\0\0\0\0\0\0";
      client.ProcessMessageString(ai, s);
      Assert.AreEqual("reset", ai.Records[0]);
    }
  }
}
