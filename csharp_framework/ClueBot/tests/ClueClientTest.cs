using System.Collections.Generic;
using System.Text.RegularExpressions;
using ClueSharp;
using NUnit.Framework;

namespace ClueBot.tests
{
  [TestFixture]
  class ClueClientTest
  {
    private DummyClient m_client;
    private DummyAI m_ai;

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
      m_ai = new DummyAI();
      m_client = new DummyClient();
    }

    [Test]
    public void TestProcessMessageString()
    {
      const string s = "reset 4 3 Gr Sc St Bi";
      m_client.ProcessMessageString(m_ai, s);
      Assert.AreEqual("reset", m_ai.Records[0]);
      Assert.AreEqual("ok", m_client.Sent[0]);
    }

    [Test]
    public void TestProcessMessageStringSuggest()
    {
      const string s = "suggest";
      m_client.ProcessMessageString(m_ai, s);
      Assert.AreEqual("suggest", m_ai.Records[0]);
      Assert.AreEqual(1, m_client.Sent.Count);
      var suggestResponseRegex = new Regex("suggest( [A-Z][a-z]){3}");
      Assert.IsTrue(suggestResponseRegex.IsMatch(m_client.Sent[0]));
    }

    [Test]
    public void TestProcessMessageStringDisprove()
    {
      const string s = "disprove 0 Sc Ca Bi";
      m_client.ProcessMessageString(m_ai, s);
      Assert.AreEqual("disprove", m_ai.Records[0]);
      Assert.AreEqual(1, m_client.Sent.Count);
      var suggestResponseRegex = new Regex("-|show( [A-Z][a-z]){0,1}");
      Assert.IsTrue(suggestResponseRegex.IsMatch(m_client.Sent[0]));
    }


    [Test]
    public void TestProcessMessageStringAccuse()
    {
      const string s = "accuse";
      m_client.ProcessMessageString(m_ai, s);
      Assert.AreEqual("accuse", m_ai.Records[0]);
      Assert.AreEqual(1, m_client.Sent.Count);
      var suggestResponseRegex = new Regex("-|accuse( [A-Z][a-z]){3}");
      Assert.IsTrue(suggestResponseRegex.IsMatch(m_client.Sent[0]));
    }

    [Test]
    public void TestProcessMessageStringWithNulls()
    {
      const string s = "reset 4 3 Gr Sc St Bi\0\0\0\0\0\0\0\0\0\0";
      m_client.ProcessMessageString(m_ai, s);
      Assert.AreEqual("reset", m_ai.Records[0]);
    }
  }
}
