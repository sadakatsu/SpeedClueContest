using System.Linq;
using NUnit.Framework;

namespace ClueSharp.tests
{
  [TestFixture]
  class MurderSetTest
  {
    [Test]
    public void TestToString()
    {
      var set = new MurderSet(Suspect.ColMustard, Weapon.Revolver, Room.Lounge);
      Assert.AreEqual("Mu Re Lo", set.ToString());
    }

    [Test]
    public void TestAllSuggestions()
    {
      const int total = (int)Suspect.Count * (int)Weapon.Count * (int)Room.Count;
      Assert.AreEqual(total, MurderSet.AllSuggestions.Count());
    }

    [Test]
    public void TestValues()
    {
      var set = new MurderSet(Suspect.ColMustard, Weapon.Revolver, Room.Lounge);
      var l = set.Values.ToList();
      Assert.AreEqual(3, l.Count());
    }

    [Test]
    public void EqualityTest()
    {
      Assert.AreEqual(
        new MurderSet(Suspect.ColMustard, Weapon.Knife, Room.Kitchen),
        new MurderSet(Suspect.ColMustard, Weapon.Knife, Room.Kitchen)
        );
    }
  }
}
