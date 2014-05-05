using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using ClueSharp;
using ClueSharp.tests;

namespace ClueByFour
{
  public class ClueByFour: IClueAI
  {
    private int m_i;
    private List<Suspect> m_suspects;
    private List<Weapon> m_weapons;
    private List<Room> m_rooms;
    private Dictionary<int, List<Card>> m_alreadyShown;
    private static readonly Random Rnd = new Random();
    private Dictionary<MurderSet, int> m_usedSuggestions;
    private Dictionary<Enum, int> m_ownedBy;

    public void Reset(int n, int i, IEnumerable<Suspect> suspects, IEnumerable<Weapon> weapons, IEnumerable<Room> rooms)
    {
      m_i = i;
      m_suspects = suspects.ToList();
      m_weapons = weapons.ToList();
      m_rooms = rooms.ToList();

      m_ownedBy = new Dictionary<Enum, int>();
      foreach (var s in m_suspects)
      {
        m_ownedBy[s] = m_i;
      }
      foreach (var w in m_weapons)
      {
        m_ownedBy[w] = m_i;
      }
      foreach (var r in m_rooms)
      {
        m_ownedBy[r] = m_i;
      }

      m_alreadyShown = new Dictionary<int, List<Card>>();
      for (int k = 0; k < n; k++)
      {
        m_alreadyShown[k] = new List<Card>();
      }

      m_usedSuggestions = new Dictionary<MurderSet, int>();
    }

    public void Suggestion(int suggester, MurderSet suggestion, int? disprover, Card disproof)
    {
      if (disproof != null)
      {
        // We know who disproved it and what they showed.
        Debug.Assert(disprover != null, "disproof is not null but disprover is null");
        m_ownedBy[disproof.Value] = (int)disprover;
      }
    }

    public void Accusation(int accuser, MurderSet suggestion, bool won)
    {
      // todo: If we know two of the cards in the murder envelope,
      // we can rule out the third one here
    }

    public MurderSet Suggest()
    {
      // todo: do this nicely?
      var maybes = Maybes;
      MurderSet randomSuggestion;
      do
      {
        randomSuggestion = GetRandomSuggestion(maybes);
      } while (m_usedSuggestions.ContainsKey(randomSuggestion));

      m_usedSuggestions[randomSuggestion] = 1;
      return randomSuggestion;
    }

    private List<Enum> Maybes
    {
      get { return Card.AllValues.Where(x => !m_ownedBy.ContainsKey(x)).ToList(); }
    }

    private static MurderSet GetRandomSuggestion(List<Enum> maybes)
    {
      if (!maybes.Any())
      {
        return new MurderSet(
          RandomEnum(Card.AllSuspects.ToList()),
          RandomEnum(Card.AllWeapons.ToList()),
          RandomEnum(Card.AllRooms.ToList())
          );
      }

      var maybe = RandomEnum(maybes);

      if (maybe is Suspect)
      {
        return new MurderSet(maybe,
                             RandomEnum(Card.AllWeapons.ToList()),
                             RandomEnum(Card.AllRooms.ToList())
          );
      }
      if (maybe is Weapon)
      {
        return new MurderSet(
          RandomEnum(Card.AllSuspects.ToList()),
          maybe,
          RandomEnum(Card.AllRooms.ToList())
          );
      }
      if (maybe is Room)
      {
        return new MurderSet(
          RandomEnum(Card.AllSuspects.ToList()),
          RandomEnum(Card.AllWeapons.ToList()),
          maybe
          );
      }
      throw new Exception("Got a value back which isn't anything.");
    }

    private static T RandomEnum<T>(List<T> l)
    {
      var n = l.Count();
      var i = Rnd.Next(0, n);
      return l[i];
    }

    public MurderSet? Accuse()
    {
      var maybes = Maybes;
      if (maybes.Count == 3)
      {
        return new MurderSet(maybes.OfType<Suspect>().Single(),
                             maybes.OfType<Weapon>().Single(),
                             maybes.OfType<Room>().Single()
          );
      }
      return null;
    }

    public Card Disprove(int player, MurderSet suggestion)
    {
      if (m_suspects.IndexOf(suggestion.Suspect) != -1)
      {
        return new Card(suggestion.Suspect);
      }
      if (m_weapons.IndexOf(suggestion.Weapon) != -1)
      {
        return new Card(suggestion.Weapon);
      }
      if (m_rooms.IndexOf(suggestion.Room) != -1)
      {
        return new Card(suggestion.Room);
      }
      return null;
    }
  }

  public class ClueByFourTest : ClueAITest<ClueByFour>
  {
  }
}
