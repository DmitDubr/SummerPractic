package validate

import (
	"regexp"
	"strings"
	"unicode/utf8"

	"github.com/dimbass/summerpractic/climbing/backend/internal/api"
	"github.com/dimbass/summerpractic/climbing/backend/internal/domain"
)

var phoneRe = regexp.MustCompile(`^\+7\d{10}$`)

func ClientContacts(c api.ClientContacts) error {
	name := strings.TrimSpace(c.Name)
	if utf8.RuneCountInString(name) < 2 || utf8.RuneCountInString(name) > 50 {
		return domain.NewAppError(domain.ErrValidation, "Укажите имя", 400)
	}
	if !phoneRe.MatchString(c.Phone) {
		return domain.NewAppError(domain.ErrValidation, "Введите корректный номер", 400)
	}
	return nil
}

func Equipment(eq api.EquipmentChoice) error {
	switch eq.Mode {
	case "OWN":
		if eq.RentalShoes || eq.RentalHarness {
			return domain.NewAppError(domain.ErrValidation, "При своём снаряжении прокат не выбирается", 400)
		}
	case "RENTAL":
		if !eq.RentalShoes && !eq.RentalHarness {
			return domain.NewAppError(domain.ErrValidation, "Выберите позиции проката", 400)
		}
	default:
		return domain.NewAppError(domain.ErrValidation, "Некорректный режим снаряжения", 400)
	}
	return nil
}

func Stars(stars int) error {
	if stars < 1 || stars > 5 {
		return domain.NewAppError(domain.ErrValidation, "Оценка от 1 до 5", 400)
	}
	return nil
}
