using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using ClueSharp;

namespace CluePaddle
{
  class CluePaddle : IClueAI
  {
    private int m_n;
    private int m_i;
    private List<Suspect> m_suspects;
    private List<Weapon> m_weapons;
    private List<Room> m_rooms;
    private CardTracker m_cardTracker;
    private Dictionary<int, List<Card>> m_alreadyShown;
    private static readonly Random Rnd = new Random();
    private Dictionary<MurderSet, int> m_usedSuggestions;

    public void Reset(int n, int i, IEnumerable<Suspect> suspects, IEnumerable<Weapon> weapons, IEnumerable<Room> rooms)
    {
      m_n = n;
      m_i = i;
      m_suspects = suspects.ToList();
      m_weapons = weapons.ToList();
      m_rooms = rooms.ToList();

      m_cardTracker = new CardTracker(n);
      m_cardTracker.SetMyCards(i, m_suspects, m_weapons, m_rooms);

      m_alreadyShown = new Dictionary<int, List<Card>>();
      for (int k = 0; k < n; k++)
      {
        m_alreadyShown[k] = new List<Card>();
      }

      m_usedSuggestions = new Dictionary<MurderSet, int>();
    }

    public void Suggestion(int suggester, MurderSet suggestion, int? disprover, Card disproof)
    {
      List<int> nonDisprovers = NonDisprovers(suggester, disprover).ToList();

      foreach (var player in nonDisprovers)
      {
        m_cardTracker.DoesntHaveAnyOf(player, suggestion);
      }

      if (disprover != null && disproof == null)
      {
        // We know who disproved it but not what they showed.
        Debug.Assert(disprover != m_i, "The disprover should see the disproof");
        Debug.Assert(suggester != m_i, "The suggester should see the disproof");
        m_cardTracker.DoesntHaveAllOf(suggester, suggestion);
        m_cardTracker.HasOneOf((int)disprover, suggestion);
      }

      if (disproof != null)
      {
        // We know who disproved it and what they showed.
        Debug.Assert(disprover != null, "disproof is not null but disprover is null");
        m_cardTracker.DoesHave((int)disprover, disproof.Value);
      }
    }

    public IEnumerable<int> NonDisprovers(int suggester, int? disprover)
    {
      int k = (suggester + 1) % m_n;

      while (k != suggester && k != disprover)
      {
        yield return k;
        k++;
        k = k % m_n;
      }
    }

    public void Accusation(int accuser, MurderSet suggestion, bool won)
    {
      if (!won)
      {
        m_cardTracker.DoesntHaveAllOf(m_n, suggestion);
      }
    }

    public MurderSet Suggest()
    {
      // todo: do this nicely?
      var maybes = m_cardTracker.Maybes;
      MurderSet randomSuggestion;
      do
      {
        randomSuggestion = GetRandomSuggestion(maybes);
      } while (m_usedSuggestions.ContainsKey(randomSuggestion));

      m_usedSuggestions[randomSuggestion] = 1;
      return randomSuggestion;
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
      m_cardTracker.ProcessInferences();
      return m_cardTracker.GetAccusation();
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
}
