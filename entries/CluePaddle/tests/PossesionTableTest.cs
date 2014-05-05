using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using ClueSharp;
using NUnit.Framework;

namespace CluePaddle.tests
{
  [TestFixture]
  class PossesionTableTest
  {
    private PossessionTable m_table;

    [SetUp]
    public void Init()
    {
      m_table = new PossessionTable(3);
    }

    [Test]
    public void NotOwnedTest()
    {
      Assert.AreEqual(false, m_table.NotOwned(Weapon.MonkeyWrench));
      m_table.DoesNotHave(0, Weapon.MonkeyWrench);
      Assert.AreEqual(false, m_table.NotOwned(Weapon.MonkeyWrench));
      m_table.DoesNotHave(1, Weapon.MonkeyWrench);
      Assert.AreEqual(false, m_table.NotOwned(Weapon.MonkeyWrench));
      m_table.DoesNotHave(2, Weapon.MonkeyWrench);
      Assert.AreEqual(true, m_table.NotOwned(Weapon.MonkeyWrench));
      m_table.DoesHave(1, Weapon.MonkeyWrench);
      Assert.AreEqual(false, m_table.NotOwned(Weapon.MonkeyWrench));
    }

    [Test]
    public void PlayerHasTwoOfTest()
    {
      var set = new MurderSet(Suspect.ProfPlum, Weapon.Candlestick, Room.Library);
      var triplet = new PlayerTriplet(1, set);
      Assert.AreEqual(null, m_table.PlayerHasTwoOf(triplet));
      m_table.DoesHave(1, Suspect.ProfPlum);
      Assert.AreEqual(null, m_table.PlayerHasTwoOf(triplet));
      m_table.DoesNotHave(0, Weapon.Candlestick);
      m_table.DoesNotHave(2, Weapon.Candlestick);
      Assert.AreEqual(null, m_table.PlayerHasTwoOf(triplet));
      m_table.DoesNotHave(0, Suspect.ProfPlum);
      m_table.DoesNotHave(2, Suspect.MrsPeacock);
      Assert.AreEqual(null, m_table.PlayerHasTwoOf(triplet));
      m_table.DoesHave(1, Weapon.Candlestick);
      m_table.DoesHave(0, Weapon.LeadPipe);
      Assert.AreEqual(Room.Library, m_table.PlayerHasTwoOf(triplet));
      m_table.DoesHave(1, Room.Conservatory);
      Assert.AreEqual(Room.Library, m_table.PlayerHasTwoOf(triplet));
      m_table.DoesHave(1, Room.Library);
      Assert.AreEqual(null, m_table.PlayerHasTwoOf(triplet));
    }

  [Test]
  public void PlayerDoesntHaveTwoOf()
  {
    var set = new MurderSet(Suspect.ProfPlum, Weapon.Candlestick, Room.Library);
    var triplet = new PlayerTriplet(1, set);
    Assert.AreEqual(null, m_table.PlayerDoesntHaveTwoOf(triplet));
    m_table.DoesNotHave(1, Room.Library);
    Assert.AreEqual(null, m_table.PlayerDoesntHaveTwoOf(triplet));
    m_table.DoesHave(2, Suspect.ProfPlum);
    Assert.AreEqual(Weapon.Candlestick, m_table.PlayerDoesntHaveTwoOf(triplet));
    m_table.DoesHave(0, Weapon.Candlestick);
    Assert.AreEqual(null, m_table.PlayerDoesntHaveTwoOf(triplet));
  }
  }
}
