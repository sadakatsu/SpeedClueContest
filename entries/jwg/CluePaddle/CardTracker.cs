using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using ClueSharp;

namespace CluePaddle
{
  internal class CardTracker
  {
    private readonly PossessionTable m_mightHave;
    private readonly List<PlayerTriplet> m_doesntHaveAllOf;
    private readonly List<PlayerTriplet> m_hasOneOf;
    private readonly int m_n;
    private int[] m_numberOfCards;

    public CardTracker(int n)
    {
      m_n = n;
      m_doesntHaveAllOf = new List<PlayerTriplet>();
      m_hasOneOf = new List<PlayerTriplet>();
      m_mightHave = new PossessionTable(n);

      SetUpNumberOfCards();
    }

    public void SetUpNumberOfCards()
    {
      m_numberOfCards = new int[m_n];

      var m = 18/m_n;
      var r = 18%m_n;

      for (int j = 0; j < m_n; j++)
      {
        NumberOfCards[j] = (j < r) ? m+1 : m;
      }
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

        changed = CheckWhatMustBeInEnvelope(changed);
        changed = CheckHowManyCardsPerPlayer(changed);
      } while (changed);
    }

    private bool CheckHowManyCardsPerPlayer(bool changed)
    {
      for (int j = 0; j < m_n; j++)
      {
        changed = changed | m_mightHave.MustHavesAndMightHaves(Card.AllValues.ToList(), NumberOfCards[j], j);
      }
      return changed;
    }

    private bool CheckWhatMustBeInEnvelope(bool changed)
    {
      changed = changed | m_mightHave.MustHavesAndMightHaves(Card.AllSuspects.Cast<Enum>().ToList(), 1,m_n);
      changed = changed | m_mightHave.MustHavesAndMightHaves(Card.AllWeapons.Cast<Enum>().ToList(), 1, m_n);
      changed = changed | m_mightHave.MustHavesAndMightHaves(Card.AllRooms.Cast<Enum>().ToList(), 1, m_n);
      return changed;
    }

    public void DoesHave(int disprover, Enum value)
    {
      m_mightHave.DoesHave(disprover, value);
    }

    public List<Enum> Maybes
    {
      get { return Card.AllValues.Where(x => m_mightHave.NotKnown(x)).ToList(); }
    }

    public int[] NumberOfCards
    {
      get { return m_numberOfCards; }
    }

    public IEnumerable<Enum> CheckForAllTargetCards(int targetPlayer)
    {
      var list = m_mightHave.MustHaves(Card.AllValues.ToList(), targetPlayer);
      return (list.Count() == m_numberOfCards[targetPlayer])
               ? list
               : null;
    }
  }
}