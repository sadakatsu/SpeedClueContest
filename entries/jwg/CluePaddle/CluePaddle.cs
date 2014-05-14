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
    private HashSet<Enum> m_neverSuggest;
    private int m_targetPlayer;
    private MurderSet? m_accusation;

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
      m_neverSuggest = new HashSet<Enum>();

      m_targetPlayer = (m_i + 1)%m_n;
      m_accusation = null;
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

        if (suggester != m_i)
        {
          m_neverSuggest.Add(disproof.Value);
        }
      }

//      if (disproof == null && suggester == m_i)
//      {
//        if (m_suspects.IndexOf(suggestion.Suspect) == -1
//            && m_weapons.IndexOf(suggestion.Weapon) == -1
//            && m_rooms.IndexOf(suggestion.Room) == -1)
//        {
//          m_accusation = suggestion;
//        }
//      }
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
      m_cardTracker.ProcessInferences();
      UpdateTargetPlayer();
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

    private MurderSet GetRandomSuggestion(List<Enum> maybes)
    {
      var maybe = RandomEnum(maybes);

      if (maybe is Suspect)
      {
        return new MurderSet(maybe,
                             RandomEnum(Card.AllWeapons),
                             RandomEnum(Card.AllRooms)
          );
      }
      if (maybe is Weapon)
      {
        return new MurderSet(
          RandomEnum(Card.AllSuspects),
          maybe,
          RandomEnum(Card.AllRooms)
          );
      }
      if (maybe is Room)
      {
        return new MurderSet(
          RandomEnum(Card.AllSuspects),
          RandomEnum(Card.AllWeapons),
          maybe
          );
      }
      throw new Exception("Got a value back which isn't anything.");
    }

    private Enum RandomEnum<T>(IEnumerable<T> l)
    {
      var list = l.Cast<Enum>()
                  .Where(x => !m_neverSuggest.Contains(x))
                  .ToList();
      var n = list.Count();
      Debug.Assert(n > 0);
      var i = Rnd.Next(0, n);
      return list[i];
    }

    public MurderSet? Accuse()
    {
//      if (m_accusation != null)
//      {
//        return m_accusation;
//      }
      m_cardTracker.ProcessInferences();
      return m_cardTracker.GetAccusation();
    }

    private void UpdateTargetPlayer()
    {
      if (m_targetPlayer != m_i)
      {
        var toIgnore = m_cardTracker.CheckForAllTargetCards(m_targetPlayer);
        if (toIgnore != null)
        {
          m_targetPlayer = (m_targetPlayer + 1)%m_n;
          foreach (var card in toIgnore)
          {
            m_neverSuggest.Add(card);
          }
        }
      }
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
