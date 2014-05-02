using System;
using System.Collections.Generic;
using System.Linq;
using ClueSharp;

namespace CluePaddle
{
  internal class CardTracker
  {
    private readonly PossessionTable m_mightHave;
    private readonly List<PlayerTriplet> m_doesntHaveAllOf;
    private readonly List<PlayerTriplet> m_hasOneOf;

    public CardTracker(int n)
    {
      m_doesntHaveAllOf = new List<PlayerTriplet>();
      m_hasOneOf = new List<PlayerTriplet>();
      m_mightHave = new PossessionTable(n);
    }

    public void SetMyCards(int i, IEnumerable<Suspect> suspects, IEnumerable<Weapon> weapons, IEnumerable<Room> rooms)
    {
      foreach (var suspect in suspects)
      {
        m_mightHave.DoesHave(i, suspect);
      }
      foreach (var weapon in weapons)
      {
        m_mightHave.DoesHave(i, weapon);
      }
      foreach (var room in rooms)
      {
        m_mightHave.DoesHave(i, room);
      }
    }

    public void DoesntHaveAnyOf(int player, MurderSet suggestion)
    {
      foreach (var value in suggestion.Values)
      {
        m_mightHave.DoesNotHave(player, value);
      }
    }

    public void DoesntHaveAllOf(int suggester, MurderSet suggestion)
    {
      m_doesntHaveAllOf.Add(new PlayerTriplet(suggester, suggestion));
    }

    public void HasOneOf(int disprover, MurderSet suggestion)
    {
      m_hasOneOf.Add(new PlayerTriplet(disprover, suggestion));
    }

    public MurderSet? GetAccusation()
    {
      var unownedCards = m_mightHave.Keys.Where(x => m_mightHave.NotOwned(x)).ToList();

      if (unownedCards.Count() != 3)
      {
        return null;
      }

      return new MurderSet(unownedCards.OfType<Suspect>().Single(),
                           unownedCards.OfType<Weapon>().Single(),
                           unownedCards.OfType<Room>().Single()
        );
    }

    public void ProcessInferences()
    {
      bool changed;
      do
      {
        changed = false;
        foreach (var triplet in m_doesntHaveAllOf.Where(x => x.m_isActive))
        {
          var mustNotHave = m_mightHave.PlayerHasTwoOf(triplet);
          if (mustNotHave != null)
          {
            changed = true;
            m_mightHave.DoesNotHave(triplet.m_player, mustNotHave);
            triplet.m_isActive = false;
          }
        }

        foreach (var triplet in m_hasOneOf.Where(x => x.m_isActive))
        {
          var mustHave = m_mightHave.PlayerDoesntHaveTwoOf(triplet);
          if (mustHave != null)
          {
            changed = true;
            m_mightHave.DoesHave(triplet.m_player, mustHave);
            triplet.m_isActive = false;
          }
        }
      } while (changed);
    }

    public void DoesHave(int disprover, Enum value)
    {
      m_mightHave.DoesHave(disprover, value);
    }

    public List<Enum> Maybes
    {
      get { return Card.AllValues.Where(x => m_mightHave.NotKnown(x)).ToList(); }
    }
  }
}